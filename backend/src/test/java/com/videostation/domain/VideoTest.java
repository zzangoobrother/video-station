package com.videostation.domain;

import com.videostation.domain.constant.VideoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VideoTest {

    private User uploader;
    private Video video;

    @BeforeEach
    void setUp() {
        uploader = User.create("admin@test.com", "encoded", "관리자", "관리자");
        video = Video.create("테스트 영상", "설명입니다", "태그1,태그2",
                "/data/originals/test.mp4", "test.mp4", 1024L, uploader);
    }

    @Test
    @DisplayName("Video 생성 시 기본값 확인 - status=UPLOADING, isPublic=false, viewCount=0")
    void createWithDefaults() {
        assertThat(video.getTitle()).isEqualTo("테스트 영상");
        assertThat(video.getDescription()).isEqualTo("설명입니다");
        assertThat(video.getTags()).isEqualTo("태그1,태그2");
        assertThat(video.getOriginalFilePath()).isEqualTo("/data/originals/test.mp4");
        assertThat(video.getOriginalFileName()).isEqualTo("test.mp4");
        assertThat(video.getFileSize()).isEqualTo(1024L);
        assertThat(video.getUploadedBy()).isEqualTo(uploader);
        assertThat(video.getStatus()).isEqualTo(VideoStatus.UPLOADING);
        assertThat(video.getIsPublic()).isFalse();
        assertThat(video.getViewCount()).isEqualTo(0L);
        assertThat(video.getHlsPath()).isNull();
        assertThat(video.getDurationSeconds()).isNull();
        assertThat(video.getThumbnailPath()).isNull();
    }

    @Test
    @DisplayName("메타데이터 수정")
    void updateMetadata() {
        video.updateMetadata("수정된 제목", "수정된 설명", "새태그");

        assertThat(video.getTitle()).isEqualTo("수정된 제목");
        assertThat(video.getDescription()).isEqualTo("수정된 설명");
        assertThat(video.getTags()).isEqualTo("새태그");
    }

    @Test
    @DisplayName("상태 변경")
    void changeStatus() {
        video.changeStatus(VideoStatus.ENCODING);

        assertThat(video.getStatus()).isEqualTo(VideoStatus.ENCODING);
    }

    @Test
    @DisplayName("공개/비공개 토글")
    void toggleVisibility() {
        assertThat(video.getIsPublic()).isFalse();

        video.toggleVisibility();
        assertThat(video.getIsPublic()).isTrue();

        video.toggleVisibility();
        assertThat(video.getIsPublic()).isFalse();
    }

    @Test
    @DisplayName("인코딩 완료 시 hlsPath, duration, thumbnail 설정 및 상태 READY 전환")
    void completeEncoding() {
        video.completeEncoding("/data/encoded/1/master.m3u8", 600, "/data/thumbnails/1.jpg");

        assertThat(video.getHlsPath()).isEqualTo("/data/encoded/1/master.m3u8");
        assertThat(video.getDurationSeconds()).isEqualTo(600);
        assertThat(video.getThumbnailPath()).isEqualTo("/data/thumbnails/1.jpg");
        assertThat(video.getStatus()).isEqualTo(VideoStatus.READY);
    }

    @Test
    @DisplayName("조회수 증가")
    void incrementViewCount() {
        video.incrementViewCount();
        video.incrementViewCount();
        video.incrementViewCount();

        assertThat(video.getViewCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Object Storage 키 설정")
    void setObjectStorageKey() {
        video.setObjectStorageKey("backup/test.mp4");

        assertThat(video.getObjectStorageKey()).isEqualTo("backup/test.mp4");
    }
}
