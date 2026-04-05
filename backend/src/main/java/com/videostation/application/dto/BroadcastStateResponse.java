package com.videostation.application.dto;

import com.videostation.domain.Broadcast;
import com.videostation.domain.PlaylistVideo;
import com.videostation.domain.Video;

import java.util.List;

public record BroadcastStateResponse(
        Long broadcastId,
        String status,
        BroadcastVideoInfo currentVideo,
        BroadcastVideoInfo nextVideo,
        long offsetSeconds,
        int currentVideoIndex,
        int totalVideosInPlaylist,
        Boolean loopPlaylist,
        String playlistName
) {
    public record BroadcastVideoInfo(
            Long id,
            String title,
            String hlsUrl,
            Integer durationSeconds,
            String thumbnailUrl
    ) {
        public static BroadcastVideoInfo from(Video video) {
            if (video == null) return null;
            return new BroadcastVideoInfo(
                    video.getId(),
                    video.getTitle(),
                    video.getHlsUrl(),
                    video.getDurationSeconds(),
                    video.getThumbnailUrl()
            );
        }
    }

    public static BroadcastStateResponse from(Broadcast broadcast) {
        List<PlaylistVideo> videos = broadcast.getPlaylist().getReadyVideos();
        int totalVideos = videos.size();
        int currentIndex = broadcast.getCurrentVideoIndex();

        Video nextVideo = null;
        if (currentIndex + 1 < totalVideos) {
            nextVideo = videos.get(currentIndex + 1).getVideo();
        } else if (broadcast.getLoopPlaylist() && !videos.isEmpty()) {
            nextVideo = videos.getFirst().getVideo();
        }

        return new BroadcastStateResponse(
                broadcast.getId(),
                broadcast.getStatus().name(),
                BroadcastVideoInfo.from(broadcast.getCurrentVideo()),
                BroadcastVideoInfo.from(nextVideo),
                broadcast.calculateCurrentOffsetSeconds(),
                currentIndex,
                totalVideos,
                broadcast.getLoopPlaylist(),
                broadcast.getPlaylist().getName()
        );
    }
}
