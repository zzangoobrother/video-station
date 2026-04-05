package com.videostation.application;

import com.videostation.application.dto.*;
import com.videostation.domain.Playlist;
import com.videostation.domain.PlaylistVideo;
import com.videostation.domain.User;
import com.videostation.domain.Video;
import com.videostation.domain.constant.VideoStatus;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.PlaylistRepository;
import com.videostation.persistence.PlaylistVideoRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @InjectMocks
    private PlaylistService playlistService;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistVideoRepository playlistVideoRepository;

    @Mock
    private VideoRepository videoRepository;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.create("admin@test.com", "encoded", "관리자", "관리자");
        ReflectionTestUtils.setField(admin, "id", 1L);
    }

    private Playlist createPlaylist(Long id, String name) {
        Playlist playlist = Playlist.create(name, "설명", false, admin);
        ReflectionTestUtils.setField(playlist, "id", id);
        return playlist;
    }

    private Video createVideo(Long id, String title) {
        Video video = Video.create(title, "설명", "태그", "/path", title + ".mp4", 1024L, admin);
        ReflectionTestUtils.setField(video, "id", id);
        video.completeEncoding("/hls/" + id + "/master.m3u8", 300, "/thumb/" + id + ".jpg");
        return video;
    }

    @Test
    @DisplayName("재생목록 생성")
    void create() {
        var request = new PlaylistRequest("새 목록", "설명", false);
        var saved = createPlaylist(1L, "새 목록");

        given(playlistRepository.save(any(Playlist.class))).willReturn(saved);

        PlaylistResponse response = playlistService.create(request, admin);

        assertThat(response.name()).isEqualTo("새 목록");
    }

    @Test
    @DisplayName("재생목록 목록 조회")
    void getList() {
        var playlist = createPlaylist(1L, "목록1");
        Page<Playlist> page = new PageImpl<>(List.of(playlist), PageRequest.of(0, 20), 1);
        given(playlistRepository.findAll(any(PageRequest.class))).willReturn(page);

        var result = playlistService.getAdminPlaylists(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("재생목록 상세 조회")
    void getDetail() {
        var playlist = createPlaylist(1L, "목록1");
        given(playlistRepository.findById(1L)).willReturn(Optional.of(playlist));

        PlaylistDetailResponse response = playlistService.getPlaylistDetail(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.videos()).isEmpty();
    }

    @Test
    @DisplayName("재생목록 상세 조회 - 존재하지 않으면 예외")
    void getDetailNotFound() {
        given(playlistRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.getPlaylistDetail(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND));
    }

    @Test
    @DisplayName("재생목록 수정")
    void update() {
        var playlist = createPlaylist(1L, "원래 이름");
        given(playlistRepository.findById(1L)).willReturn(Optional.of(playlist));

        PlaylistResponse response = playlistService.update(1L, new PlaylistRequest("수정됨", "수정 설명", true));

        assertThat(response.name()).isEqualTo("수정됨");
        assertThat(response.isPublic()).isTrue();
    }

    @Test
    @DisplayName("재생목록 삭제")
    void delete() {
        var playlist = createPlaylist(1L, "목록");
        given(playlistRepository.findById(1L)).willReturn(Optional.of(playlist));

        playlistService.delete(1L);

        then(playlistRepository).should().delete(playlist);
    }

    @Test
    @DisplayName("재생목록에 영상 추가")
    void addVideo() {
        var playlist = createPlaylist(1L, "목록");
        var video = createVideo(10L, "영상A");

        given(playlistRepository.findById(1L)).willReturn(Optional.of(playlist));
        given(videoRepository.findById(10L)).willReturn(Optional.of(video));
        given(playlistVideoRepository.countByPlaylistId(1L)).willReturn(0);

        PlaylistDetailResponse response = playlistService.addVideo(1L, 10L);

        assertThat(playlist.getPlaylistVideos()).hasSize(1);
    }

    @Test
    @DisplayName("재생목록에서 영상 제거")
    void removeVideo() {
        var playlist = createPlaylist(1L, "목록");
        var video = createVideo(10L, "영상A");
        var pv = PlaylistVideo.create(playlist, video, 0);
        ReflectionTestUtils.setField(pv, "id", 100L);
        playlist.getPlaylistVideos().add(pv);

        given(playlistVideoRepository.findByPlaylistIdAndVideoId(1L, 10L)).willReturn(Optional.of(pv));

        playlistService.removeVideo(1L, 10L);

        then(playlistVideoRepository).should().delete(pv);
    }

    @Test
    @DisplayName("재생목록 영상 순서 변경")
    void reorder() {
        var playlist = createPlaylist(1L, "목록");
        var videoA = createVideo(10L, "영상A");
        var videoB = createVideo(20L, "영상B");

        var pvA = PlaylistVideo.create(playlist, videoA, 0);
        var pvB = PlaylistVideo.create(playlist, videoB, 1);
        ReflectionTestUtils.setField(pvA, "id", 100L);
        ReflectionTestUtils.setField(pvB, "id", 101L);

        given(playlistRepository.findById(1L)).willReturn(Optional.of(playlist));
        given(playlistVideoRepository.findByPlaylistIdOrderBySortOrderAsc(1L)).willReturn(List.of(pvA, pvB));

        // 순서 뒤집기: B → A
        PlaylistDetailResponse response = playlistService.reorder(1L, new ReorderRequest(List.of(20L, 10L)));

        assertThat(pvB.getSortOrder()).isEqualTo(0);
        assertThat(pvA.getSortOrder()).isEqualTo(1);
    }
}
