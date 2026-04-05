package com.videostation.application.dto;

import com.videostation.domain.Playlist;

import java.time.LocalDateTime;

public record PlaylistResponse(
        Long id,
        String name,
        String description,
        String thumbnailUrl,
        Boolean isPublic,
        int videoCount,
        int totalDurationSeconds,
        UserResponse createdBy,
        LocalDateTime createdAt
) {
    public static PlaylistResponse from(Playlist playlist) {
        return new PlaylistResponse(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                playlist.getThumbnailUrl(),
                playlist.getIsPublic(),
                playlist.getPlaylistVideos().size(),
                playlist.getTotalDurationSeconds(),
                UserResponse.from(playlist.getCreatedByUser()),
                playlist.getCreatedAt()
        );
    }
}
