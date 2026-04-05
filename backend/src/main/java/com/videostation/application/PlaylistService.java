package com.videostation.application;

import com.videostation.application.dto.*;
import com.videostation.domain.Playlist;
import com.videostation.domain.PlaylistVideo;
import com.videostation.domain.User;
import com.videostation.domain.Video;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.PlaylistRepository;
import com.videostation.persistence.PlaylistVideoRepository;
import com.videostation.persistence.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistVideoRepository playlistVideoRepository;
    private final VideoRepository videoRepository;

    @Transactional
    public PlaylistResponse create(PlaylistRequest request, User creator) {
        Playlist playlist = Playlist.create(
                request.name(), request.description(),
                request.isPublic() != null && request.isPublic(), creator
        );
        return PlaylistResponse.from(playlistRepository.save(playlist));
    }

    public Page<PlaylistResponse> getAdminPlaylists(Pageable pageable) {
        return playlistRepository.findAll(pageable).map(PlaylistResponse::from);
    }

    public PlaylistDetailResponse getPlaylistDetail(Long playlistId) {
        return PlaylistDetailResponse.from(findPlaylist(playlistId));
    }

    @Transactional
    public PlaylistResponse update(Long playlistId, PlaylistRequest request) {
        Playlist playlist = findPlaylist(playlistId);
        playlist.update(request.name(), request.description(),
                request.isPublic() != null && request.isPublic());
        return PlaylistResponse.from(playlist);
    }

    @Transactional
    public void delete(Long playlistId) {
        Playlist playlist = findPlaylist(playlistId);
        playlistRepository.delete(playlist);
    }

    @Transactional
    public PlaylistDetailResponse addVideo(Long playlistId, Long videoId) {
        Playlist playlist = findPlaylist(playlistId);
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        int nextOrder = playlistVideoRepository.countByPlaylistId(playlistId);
        PlaylistVideo pv = PlaylistVideo.create(playlist, video, nextOrder);
        playlist.getPlaylistVideos().add(pv);

        return PlaylistDetailResponse.from(playlist);
    }

    @Transactional
    public void removeVideo(Long playlistId, Long videoId) {
        PlaylistVideo pv = playlistVideoRepository.findByPlaylistIdAndVideoId(playlistId, videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
        playlistVideoRepository.delete(pv);
    }

    @Transactional
    public PlaylistDetailResponse reorder(Long playlistId, ReorderRequest request) {
        Playlist playlist = findPlaylist(playlistId);
        List<PlaylistVideo> existing = playlistVideoRepository.findByPlaylistIdOrderBySortOrderAsc(playlistId);

        Map<Long, PlaylistVideo> byVideoId = existing.stream()
                .collect(Collectors.toMap(pv -> pv.getVideo().getId(), Function.identity()));

        List<Long> videoIds = request.videoIds();
        for (int i = 0; i < videoIds.size(); i++) {
            PlaylistVideo pv = byVideoId.get(videoIds.get(i));
            if (pv != null) {
                pv.changeSortOrder(i);
            }
        }

        return PlaylistDetailResponse.from(playlist);
    }

    private Playlist findPlaylist(Long playlistId) {
        return playlistRepository.findById(playlistId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    }
}
