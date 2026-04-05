package com.videostation.application.dto;

import com.videostation.domain.Playlist;

import java.time.LocalDateTime;
import java.util.List;

public record PlaylistDetailResponse(
        Long id,
        String name,
        String description,
        String thumbnailUrl,
        Boolean isPublic,
        int videoCount,
        int totalDurationSeconds,
        UserResponse createdBy,
        LocalDateTime createdAt,
        List<PlaylistVideoResponse> videos
) {
    public static PlaylistDetailResponse from(Playlist playlist) {
        var videos = playlist.getPlaylistVideos().stream()
                .map(pv -> new PlaylistVideoResponse(pv.getSortOrder(), VideoResponse.from(pv.getVideo())))
                .toList();

        int totalDuration = playlist.getPlaylistVideos().stream()
                .filter(pv -> pv.getVideo().getDurationSeconds() != null)
                .mapToInt(pv -> pv.getVideo().getDurationSeconds())
                .sum();

        return new PlaylistDetailResponse(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                playlist.getThumbnailUrl(),
                playlist.getIsPublic(),
                videos.size(),
                totalDuration,
                UserResponse.from(playlist.getCreatedByUser()),
                playlist.getCreatedAt(),
                videos
        );
    }
}
