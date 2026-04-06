package com.videostation.application;

import com.videostation.application.dto.BroadcastStartRequest;
import com.videostation.application.dto.BroadcastStateResponse;
import com.videostation.broadcast.BroadcastStateManager;
import com.videostation.domain.*;
import com.videostation.domain.constant.BroadcastStatus;
import com.videostation.domain.constant.ChannelStatus;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.BroadcastRepository;
import com.videostation.persistence.ChannelRepository;
import com.videostation.persistence.PlaylistRepository;
import com.videostation.persistence.UserRepository;
import com.videostation.websocket.BroadcastWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BroadcastService {

    private final BroadcastRepository broadcastRepository;
    private final ChannelRepository channelRepository;
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final ChannelService channelService;
    private final BroadcastStateManager stateManager;
    private final BroadcastWebSocketHandler webSocketHandler;

    @Transactional
    public BroadcastStateResponse start(Long channelId, BroadcastStartRequest request, Long userId) {
        Channel channel = channelService.findChannel(channelId);

        broadcastRepository.findByChannelIdAndStatusIn(channelId,
                List.of(BroadcastStatus.LIVE, BroadcastStatus.PAUSED))
                .ifPresent(b -> { throw new BusinessException(ErrorCode.BROADCAST_ALREADY_LIVE); });

        Playlist playlist = playlistRepository.findById(request.playlistId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

        List<PlaylistVideo> readyVideos = playlist.getReadyVideos();
        if (readyVideos.isEmpty()) {
            throw new BusinessException(ErrorCode.PLAYLIST_EMPTY);
        }

        User user = userRepository.getReferenceById(userId);
        boolean loop = request.loopPlaylist() != null && request.loopPlaylist();
        Broadcast broadcast = Broadcast.create(channel, playlist, loop, user);
        broadcast.start(readyVideos.getFirst().getVideo());
        broadcastRepository.save(broadcast);

        channel.changeStatus(ChannelStatus.LIVE);
        channelRepository.save(channel);

        stateManager.register(broadcast.getId());
        log.info("방송 시작 - 채널: {}, 재생목록: {}", channel.getName(), playlist.getName());

        return buildAndBroadcast(broadcast);
    }

    @Transactional
    public BroadcastStateResponse pause(Long channelId) {
        Broadcast broadcast = findActiveBroadcast(channelId);
        if (broadcast.getStatus() != BroadcastStatus.LIVE) {
            throw new BusinessException(ErrorCode.BROADCAST_NOT_LIVE);
        }

        broadcast.pause();
        broadcast.getChannel().changeStatus(ChannelStatus.PAUSED);
        log.info("방송 일시정지 - 채널: {}", broadcast.getChannel().getName());
        return buildAndBroadcast(broadcast);
    }

    @Transactional
    public BroadcastStateResponse resume(Long channelId) {
        Broadcast broadcast = findActiveBroadcast(channelId);
        if (broadcast.getStatus() != BroadcastStatus.PAUSED) {
            throw new BusinessException(ErrorCode.BROADCAST_NOT_PAUSED);
        }

        broadcast.resume();
        broadcast.getChannel().changeStatus(ChannelStatus.LIVE);
        log.info("방송 재개 - 채널: {}", broadcast.getChannel().getName());
        return buildAndBroadcast(broadcast);
    }

    @Transactional
    public void stop(Long channelId) {
        Broadcast broadcast = findActiveBroadcast(channelId);
        broadcast.end();
        broadcast.getChannel().changeStatus(ChannelStatus.IDLE);
        stateManager.unregister(broadcast.getId());
        log.info("방송 종료 - 채널: {}", broadcast.getChannel().getName());
        buildAndBroadcast(broadcast);
    }

    @Transactional
    public BroadcastStateResponse next(Long channelId) {
        return advance(channelId, 1);
    }

    @Transactional
    public BroadcastStateResponse previous(Long channelId) {
        return advance(channelId, -1);
    }

    @Transactional
    public BroadcastStateResponse jump(Long channelId, int index) {
        Broadcast broadcast = findActiveBroadcast(channelId);
        List<PlaylistVideo> videos = broadcast.getPlaylist().getReadyVideos();

        if (index < 0 || index >= videos.size()) {
            throw new BusinessException(ErrorCode.BROADCAST_INVALID_INDEX);
        }

        broadcast.switchVideo(videos.get(index).getVideo(), index);
        return buildAndBroadcast(broadcast);
    }

    public BroadcastStateResponse getState(Long channelId) {
        Broadcast broadcast = findActiveBroadcast(channelId);
        return BroadcastStateResponse.from(broadcast);
    }

    public BroadcastStateResponse getLiveBroadcast() {
        return broadcastRepository.findByStatusIn(List.of(BroadcastStatus.LIVE, BroadcastStatus.PAUSED))
                .stream()
                .findFirst()
                .map(BroadcastStateResponse::from)
                .orElse(null);
    }

    /**
     * 스케줄러에서 호출: 다음 영상으로 자동 전환, 재생목록 끝이면 반복 또는 종료
     */
    @Transactional
    public boolean advanceIfEnded(Broadcast broadcast) {
        if (broadcast.getStatus() != BroadcastStatus.LIVE) {
            return false;
        }
        if (broadcast.getCurrentVideo() == null || broadcast.getCurrentVideo().getDurationSeconds() == null) {
            return false;
        }

        long offset = broadcast.calculateCurrentOffsetSeconds();
        int duration = broadcast.getCurrentVideo().getDurationSeconds();
        if (offset < duration) {
            return false;
        }

        List<PlaylistVideo> readyVideos = broadcast.getPlaylist().getReadyVideos();
        int nextIndex = broadcast.getCurrentVideoIndex() + 1;

        if (nextIndex >= readyVideos.size()) {
            if (broadcast.getLoopPlaylist()) {
                nextIndex = 0;
                log.info("재생목록 반복 - 채널: {}", broadcast.getChannel().getName());
            } else {
                broadcast.end();
                broadcast.getChannel().changeStatus(ChannelStatus.IDLE);
                stateManager.unregister(broadcast.getId());
                log.info("재생목록 종료로 방송 자동 종료 - 채널: {}", broadcast.getChannel().getName());
                buildAndBroadcast(broadcast);
                return true;
            }
        }

        broadcast.switchVideo(readyVideos.get(nextIndex).getVideo(), nextIndex);
        log.info("자동 영상 전환 - 채널: {}, 영상: {}", broadcast.getChannel().getName(),
                broadcast.getCurrentVideo().getTitle());
        buildAndBroadcast(broadcast);
        return true;
    }

    Broadcast findActiveBroadcast(Long channelId) {
        return broadcastRepository.findByChannelIdAndStatusIn(channelId,
                        List.of(BroadcastStatus.LIVE, BroadcastStatus.PAUSED))
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_LIVE));
    }

    private BroadcastStateResponse advance(Long channelId, int direction) {
        Broadcast broadcast = findActiveBroadcast(channelId);
        List<PlaylistVideo> videos = broadcast.getPlaylist().getReadyVideos();
        int targetIndex = broadcast.getCurrentVideoIndex() + direction;

        if (targetIndex < 0 || targetIndex >= videos.size()) {
            if (broadcast.getLoopPlaylist()) {
                targetIndex = direction > 0 ? 0 : videos.size() - 1;
            } else {
                throw new BusinessException(ErrorCode.BROADCAST_INVALID_INDEX);
            }
        }

        broadcast.switchVideo(videos.get(targetIndex).getVideo(), targetIndex);
        return buildAndBroadcast(broadcast);
    }

    private BroadcastStateResponse buildAndBroadcast(Broadcast broadcast) {
        BroadcastStateResponse response = BroadcastStateResponse.from(broadcast);
        webSocketHandler.broadcastState(response);
        return response;
    }
}
