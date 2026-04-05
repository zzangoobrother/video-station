package com.videostation.event;

public record VideoUploadedEvent(Long videoId, String originalFilePath) {}
