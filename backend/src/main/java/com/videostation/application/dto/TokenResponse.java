package com.videostation.application.dto;

public record TokenResponse(
        String accessToken,
        int expiresIn
) {}
