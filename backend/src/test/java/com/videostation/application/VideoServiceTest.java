package com.videostation.application;

import com.videostation.application.dto.VideoResponse;
import com.videostation.application.dto.VideoUpdateRequest;
import com.videostation.domain.User;
import com.videostation.domain.Video;
import com.videostation.domain.constant.VideoStatus;
import com.videostation.event.VideoUploadedEvent;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.UserRepository;
import com.videostation.persistence.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @InjectMocks
    private VideoService videoService;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.create("admin@test.com", "encoded", "관리자", "관리자");
        ReflectionTestUtils.setField(admin, "id", 1L);
    }

    private Video createVideo(Long id, String title, VideoStatus status) {
        Video video = Video.create(title, "설명", "태그", "/path/" + title + ".mp4", title + ".mp4", 1024L, admin);
        ReflectionTestUtils.setField(video, "id", id);
        if (status != VideoStatus.UPLOADING) {
            video.changeStatus(status);
        }
        return video;
    }

    @Test
    @DisplayName("동영상 업로드 - 파일 저장 후 Video 엔티티 생성 및 이벤트 발행")
    void upload() {
        var file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());
        var savedVideo = createVideo(1L, "테스트 영상", VideoStatus.ENCODING_QUEUED);

        given(userRepository.getReferenceById(1L)).willReturn(admin);
        given(fileStorageService.storeOriginal(file)).willReturn(java.nio.file.Path.of("/data/videos/originals/test.mp4"));
        given(videoRepository.save(any(Video.class))).willReturn(savedVideo);

        VideoResponse response = videoService.upload(file, "테스트 영상", "설명", "태그", 1L);

        assertThat(response.title()).isEqualTo("테스트 영상");
        then(eventPublisher).should().publishEvent(any(VideoUploadedEvent.class));
    }

    @Test
    @DisplayName("관리자 영상 목록 조회 - 페이징")
    void getAdminVideos() {
        var video = createVideo(1L, "영상1", VideoStatus.READY);
        Page<Video> page = new PageImpl<>(List.of(video), PageRequest.of(0, 20), 1);
        given(videoRepository.findAll(any(PageRequest.class))).willReturn(page);

        var result = videoService.getAdminVideos(null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("관리자 영상 목록 조회 - 상태 필터")
    void getAdminVideosByStatus() {
        var video = createVideo(1L, "영상1", VideoStatus.READY);
        Page<Video> page = new PageImpl<>(List.of(video), PageRequest.of(0, 20), 1);
        given(videoRepository.findByStatus(VideoStatus.READY, PageRequest.of(0, 20))).willReturn(page);

        var result = videoService.getAdminVideos(VideoStatus.READY, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("영상 상세 조회")
    void getVideo() {
        var video = createVideo(1L, "영상1", VideoStatus.READY);
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));

        VideoResponse response = videoService.getVideo(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("영상1");
    }

    @Test
    @DisplayName("영상 상세 조회 - 존재하지 않으면 예외")
    void getVideoNotFound() {
        given(videoRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.getVideo(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VIDEO_NOT_FOUND));
    }

    @Test
    @DisplayName("메타데이터 수정")
    void updateMetadata() {
        var video = createVideo(1L, "원래 제목", VideoStatus.READY);
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));

        VideoResponse response = videoService.updateVideo(1L, new VideoUpdateRequest("수정 제목", "수정 설명", "수정 태그"));

        assertThat(response.title()).isEqualTo("수정 제목");
    }

    @Test
    @DisplayName("영상 삭제 - soft delete")
    void deleteVideo() {
        var video = createVideo(1L, "영상1", VideoStatus.READY);
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));

        videoService.deleteVideo(1L);

        assertThat(video.getStatus()).isEqualTo(VideoStatus.DELETED);
    }

    @Test
    @DisplayName("공개/비공개 전환")
    void toggleVisibility() {
        var video = createVideo(1L, "영상1", VideoStatus.READY);
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));

        VideoResponse response = videoService.toggleVisibility(1L);

        assertThat(response.isPublic()).isTrue();
    }

    @Test
    @DisplayName("인코딩 상태 조회")
    void getEncodingStatus() {
        var video = createVideo(1L, "영상1", VideoStatus.ENCODING);
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));

        VideoResponse response = videoService.getVideo(1L);

        assertThat(response.status()).isEqualTo("ENCODING");
    }
}
