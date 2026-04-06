package com.videostation.application.dto;

import com.videostation.domain.Channel;

import java.time.LocalDateTime;

public record ChannelResponse(
        Long id,
        String name,
        String description,
        String thumbnailUrl,
        String status,
        UserResponse owner,
        LocalDateTime createdAt
) {
    public static ChannelResponse from(Channel channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getName(),
                channel.getDescription(),
                channel.getThumbnailUrl(),
                channel.getStatus().name(),
                UserResponse.from(channel.getOwner()),
                channel.getCreatedAt()
        );
    }
}
