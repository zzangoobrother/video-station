package com.videostation.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${storage.encoded-path}")
    private String encodedPath;

    @Value("${storage.thumbnails-path}")
    private String thumbnailsPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // HLS 파일 서빙: /hls/{videoId}/master.m3u8 → 로컬 파일 시스템
        registry.addResourceHandler("/hls/**")
                .addResourceLocations("file:" + encodedPath + "/");

        // 썸네일 서빙: /thumbnails/{id}.jpg → 로컬 파일 시스템
        registry.addResourceHandler("/thumbnails/**")
                .addResourceLocations("file:" + thumbnailsPath + "/");
    }
}
