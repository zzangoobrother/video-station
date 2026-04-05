package com.videostation.application;

import com.videostation.application.dto.PlaylistDetailResponse;
import com.videostation.application.dto.PlaylistResponse;
import com.videostation.application.dto.VideoResponse;
import com.videostation.domain.Playlist;
import com.videostation.domain.User;
import com.videostation.domain.Video;
import com.videostation.domain.constant.VideoStatus;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.PlaylistRepository;
import com.videostation.persistence.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ViewerServiceTest {

    @InjectMocks
    private ViewerService viewerService;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private PlaylistRepository playlistRepository;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.create("admin@test.com", "encoded", "관리자", "관리자");
        ReflectionTestUtils.setField(admin, "id", 1L);
    }

    private Video createReadyVideo(Long id, String title) {
        Video video = Video.create(title, "설명", "태그", "/path", title + ".mp4", 1024L, admin);
        ReflectionTestUtils.setField(video, "id", id);
        video.completeEncoding("/hls/" + id + "/master.m3u8", 300, "/thumb/" + id + ".jpg");
        return video;
    }

    @Test
    @DisplayName("공개 영상 목록 조회")
    void getPublicVideos() {
        var video = createReadyVideo(1L, "영상1");
        Page<Video> page = new PageImpl<>(List.of(video), PageRequest.of(0, 20), 1);
        given(videoRepository.findByIsPublicTrueAndStatus(eq(VideoStatus.READY), any())).willReturn(page);

        var result = viewerService.getPublicVideos(null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("영상1");
    }

    @Test
    @DisplayName("공개 영상 목록 - 키워드 검색")
    void getPublicVideosWithKeyword() {
        var video = createReadyVideo(1L, "검색 영상");
        Page<Video> page = new PageImpl<>(List.of(video), PageRequest.of(0, 20), 1);
        given(videoRepository.findByIsPublicTrueAndStatusAndTitleContaining(
                eq(VideoStatus.READY), eq("검색"), any())).willReturn(page);

        var result = viewerService.getPublicVideos("검색", PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("영상 재생 정보 조회 + 조회수 증가")
    void getVideoForPlay() {
        var video = createReadyVideo(1L, "영상1");
        video.toggleVisibility();
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));

        VideoResponse response = viewerService.getVideoForPlay(1L);

        assertThat(response.id()).isEqualTo(1L);
        then(videoRepository).should().incrementViewCount(1L);
    }

    @Test
    @DisplayName("비공개 영상 재생 시 예외")
    void getVideoForPlayNotPublic() {
        var video = createReadyVideo(1L, "비공개 영상");
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));

        assertThatThrownBy(() -> viewerService.getVideoForPlay(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VIDEO_NOT_FOUND));
    }

    @Test
    @DisplayName("공개 재생목록 목록 조회")
    void getPublicPlaylists() {
        var playlist = Playlist.create("목록1", "설명", true, admin);
        ReflectionTestUtils.setField(playlist, "id", 1L);
        Page<Playlist> page = new PageImpl<>(List.of(playlist), PageRequest.of(0, 20), 1);
        given(playlistRepository.findByIsPublicTrue(any())).willReturn(page);

        var result = viewerService.getPublicPlaylists(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("재생목록 상세 조회 - 비공개면 예외")
    void getPlaylistDetailNotPublic() {
        var playlist = Playlist.create("비공개", "설명", false, admin);
        ReflectionTestUtils.setField(playlist, "id", 1L);
        given(playlistRepository.findById(1L)).willReturn(Optional.of(playlist));

        assertThatThrownBy(() -> viewerService.getPublicPlaylistDetail(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND));
    }
}
