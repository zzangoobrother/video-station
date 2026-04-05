package com.videostation.encoding;

import com.videostation.domain.User;
import com.videostation.domain.Video;
import com.videostation.domain.constant.VideoStatus;
import com.videostation.event.EncodingCompletedEvent;
import com.videostation.event.EncodingFailedEvent;
import com.videostation.event.VideoUploadedEvent;
import com.videostation.persistence.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class EncodingEventListenerTest {

    @InjectMocks
    private EncodingEventListener listener;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private EncodingQueue encodingQueue;

    private Video video;

    @BeforeEach
    void setUp() {
        User admin = User.create("admin@test.com", "encoded", "관리자", "관리자");
        ReflectionTestUtils.setField(admin, "id", 1L);
        video = Video.create("영상1", "설명", "태그", "/path/test.mp4", "test.mp4", 1024L, admin);
        ReflectionTestUtils.setField(video, "id", 1L);
    }

    @Test
    @DisplayName("업로드 완료 이벤트 수신 시 인코딩 큐에 추가")
    void onVideoUploaded() {
        var event = new VideoUploadedEvent(1L, "/path/test.mp4");

        listener.onVideoUploaded(event);

        then(encodingQueue).should().enqueue(1L, "/path/test.mp4");
    }

    @Test
    @DisplayName("인코딩 완료 이벤트 수신 시 Video 상태 READY로 변경")
    void onEncodingCompleted() {
        var event = new EncodingCompletedEvent(1L, "/data/encoded/1/master.m3u8", 600, "/data/thumbnails/1.jpg");
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));

        listener.onEncodingCompleted(event);

        assertThat(video.getStatus()).isEqualTo(VideoStatus.READY);
        assertThat(video.getHlsPath()).isEqualTo("/data/encoded/1/master.m3u8");
        assertThat(video.getDurationSeconds()).isEqualTo(600);
    }

    @Test
    @DisplayName("인코딩 실패 이벤트 수신 시 Video 상태 FAILED로 변경")
    void onEncodingFailed() {
        var event = new EncodingFailedEvent(1L, "FFmpeg 에러");
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));

        listener.onEncodingFailed(event);

        assertThat(video.getStatus()).isEqualTo(VideoStatus.FAILED);
    }
}
