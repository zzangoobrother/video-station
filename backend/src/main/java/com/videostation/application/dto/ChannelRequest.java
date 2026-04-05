package com.videostation.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ChannelRequest(
        @NotBlank String name,
        String description
) {}
