package com.videostation.event;

public record EncodingFailedEvent(Long videoId, String reason) {}
