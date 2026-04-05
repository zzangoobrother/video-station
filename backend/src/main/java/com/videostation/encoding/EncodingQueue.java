package com.videostation.encoding;

import com.videostation.event.EncodingCompletedEvent;
import com.videostation.event.EncodingFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Slf4j
@Component
@RequiredArgsConstructor
public class EncodingQueue {

    private final FFmpegEncoder ffmpegEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${encoding.max-concurrent}")
    private int maxConcurrent;

    private final Semaphore semaphore = new Semaphore(2);

    @Async
    public void enqueue(Long videoId, String originalFilePath) {
        try {
            semaphore.acquire();
            log.info("인코딩 시작: videoId={}", videoId);

            // 영상 길이 추출
            Integer duration = ffmpegEncoder.probeDuration(originalFilePath);

            // HLS 인코딩
            String[] encodeCommand = ffmpegEncoder.buildEncodeCommand(videoId, originalFilePath);
            int exitCode = ffmpegEncoder.execute(encodeCommand);

            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg 인코딩 실패: exit code " + exitCode);
            }

            // 썸네일 추출
            String[] thumbnailCommand = ffmpegEncoder.buildThumbnailCommand(videoId, originalFilePath);
            ffmpegEncoder.execute(thumbnailCommand);

            String hlsPath = ffmpegEncoder.getHlsPath(videoId);
            String thumbnailPath = ffmpegEncoder.getThumbnailPath(videoId);

            log.info("인코딩 완료: videoId={}", videoId);
            eventPublisher.publishEvent(new EncodingCompletedEvent(videoId, hlsPath, duration, thumbnailPath));

        } catch (Exception e) {
            log.error("인코딩 실패: videoId={}", videoId, e);
            eventPublisher.publishEvent(new EncodingFailedEvent(videoId, e.getMessage()));
        } finally {
            semaphore.release();
        }
    }
}
