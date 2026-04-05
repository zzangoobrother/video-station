package com.videostation.application.dto;

import com.videostation.domain.Video;

import java.time.LocalDateTime;

public record VideoResponse(
        Long id,
        String title,
        String description,
        String originalFileName,
        Long fileSize,
        String thumbnailUrl,
        Integer durationSeconds,
        String status,
        String hlsUrl,
        String tags,
        Boolean isPublic,
        Long viewCount,
        UserResponse uploadedBy,
        LocalDateTime createdAt
) {
    public static VideoResponse from(Video video) {
        return new VideoResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getOriginalFileName(),
                video.getFileSize(),
                video.getThumbnailUrl(),
                video.getDurationSeconds(),
                video.getStatus().name(),
                video.getHlsUrl(),
                video.getTags(),
                video.getIsPublic(),
                video.getViewCount(),
                UserResponse.from(video.getUploadedBy()),
                video.getCreatedAt()
        );
    }
}
