package com.videostation.application;

import com.videostation.application.dto.PlaylistDetailResponse;
import com.videostation.application.dto.PlaylistResponse;
import com.videostation.application.dto.VideoResponse;
import com.videostation.domain.Playlist;
import com.videostation.domain.Video;
import com.videostation.domain.constant.VideoStatus;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.PlaylistRepository;
import com.videostation.persistence.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViewerService {

    private final VideoRepository videoRepository;
    private final PlaylistRepository playlistRepository;

    public Page<VideoResponse> getPublicVideos(String keyword, Pageable pageable) {
        Page<Video> page;
        if (keyword != null && !keyword.isBlank()) {
            page = videoRepository.findByIsPublicTrueAndStatusAndTitleContaining(
                    VideoStatus.READY, keyword, pageable);
        } else {
            page = videoRepository.findByIsPublicTrueAndStatus(VideoStatus.READY, pageable);
        }
        return page.map(VideoResponse::from);
    }

    @Transactional
    public VideoResponse getVideoForPlay(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        if (!video.getIsPublic() || video.getStatus() != VideoStatus.READY) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }

        videoRepository.incrementViewCount(videoId);
        return VideoResponse.from(video);
    }

    public Page<PlaylistResponse> getPublicPlaylists(Pageable pageable) {
        return playlistRepository.findByIsPublicTrue(pageable).map(PlaylistResponse::from);
    }

    public PlaylistDetailResponse getPublicPlaylistDetail(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

        if (!playlist.getIsPublic()) {
            throw new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND);
        }

        return PlaylistDetailResponse.from(playlist);
    }
}
