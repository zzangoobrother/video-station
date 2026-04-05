package com.videostation.application.dto;

import com.videostation.domain.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String role,
        String status,
        String profileImageUrl,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getProfileImageUrl(),
                user.getCreatedAt()
        );
    }
}
