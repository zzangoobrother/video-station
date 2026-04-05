package com.videostation.application.dto;

public record PlaylistVideoResponse(
        int sortOrder,
        VideoResponse video
) {}
