package com.videostation.encoding;

import com.videostation.event.EncodingCompletedEvent;
import com.videostation.event.EncodingFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Slf4j
@Component
public class EncodingQueue {

    private final FFmpegEncoder ffmpegEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Semaphore semaphore;

    public EncodingQueue(FFmpegEncoder ffmpegEncoder,
                         ApplicationEventPublisher eventPublisher,
                         @Value("${encoding.max-concurrent}") int maxConcurrent) {
        this.ffmpegEncoder = ffmpegEncoder;
        this.eventPublisher = eventPublisher;
        this.semaphore = new Semaphore(maxConcurrent);
    }

    @Async
    public void enqueue(Long videoId, String originalFilePath) {
        try {
            semaphore.acquire();
            log.info("인코딩 시작: videoId={}", videoId);

            Integer duration = ffmpegEncoder.probeDuration(originalFilePath);

            String[] encodeCommand = ffmpegEncoder.buildEncodeCommand(videoId, originalFilePath);
            int exitCode = ffmpegEncoder.execute(encodeCommand);

            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg 인코딩 실패: exit code " + exitCode);
            }

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
