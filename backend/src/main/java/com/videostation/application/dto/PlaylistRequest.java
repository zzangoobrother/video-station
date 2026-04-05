package com.videostation.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaylistRequest(
        @NotBlank String name,
        String description,
        Boolean isPublic
) {}
