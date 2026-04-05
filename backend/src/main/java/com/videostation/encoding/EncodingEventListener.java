package com.videostation.encoding;

import com.videostation.domain.Video;
import com.videostation.domain.constant.VideoStatus;
import com.videostation.event.EncodingCompletedEvent;
import com.videostation.event.EncodingFailedEvent;
import com.videostation.event.VideoUploadedEvent;
import com.videostation.persistence.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EncodingEventListener {

    private final VideoRepository videoRepository;
    private final EncodingQueue encodingQueue;

    @EventListener
    public void onVideoUploaded(VideoUploadedEvent event) {
        log.info("영상 업로드 이벤트 수신: videoId={}", event.videoId());
        encodingQueue.enqueue(event.videoId(), event.originalFilePath());
    }

    @EventListener
    @Transactional
    public void onEncodingCompleted(EncodingCompletedEvent event) {
        log.info("인코딩 완료 이벤트 수신: videoId={}", event.videoId());
        videoRepository.findById(event.videoId()).ifPresent(video ->
                video.completeEncoding(event.hlsPath(), event.durationSeconds(), event.thumbnailPath())
        );
    }

    @EventListener
    @Transactional
    public void onEncodingFailed(EncodingFailedEvent event) {
        log.error("인코딩 실패 이벤트 수신: videoId={}, reason={}", event.videoId(), event.reason());
        videoRepository.findById(event.videoId()).ifPresent(video ->
                video.changeStatus(VideoStatus.FAILED)
        );
    }
}
