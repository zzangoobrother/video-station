package com.videostation.application.dto;

import jakarta.validation.constraints.NotNull;

public record BroadcastStartRequest(
        @NotNull Long playlistId,
        Boolean loopPlaylist
) {}
