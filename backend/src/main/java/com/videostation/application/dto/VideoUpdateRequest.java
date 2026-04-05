package com.videostation.application.dto;

import jakarta.validation.constraints.NotBlank;

public record VideoUpdateRequest(
        @NotBlank String title,
        String description,
        String tags
) {}
