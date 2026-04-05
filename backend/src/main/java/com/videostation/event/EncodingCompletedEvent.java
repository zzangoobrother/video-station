package com.videostation.event;

public record EncodingCompletedEvent(Long videoId, String hlsPath, Integer durationSeconds, String thumbnailPath) {}
