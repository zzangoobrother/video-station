package com.videostation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistTest {

    @Test
    @DisplayName("Playlist 생성 시 기본값 확인")
    void createWithDefaults() {
        User creator = User.create("admin@test.com", "encoded", "관리자", "관리자");
        Playlist playlist = Playlist.create("재생목록1", "설명", false, creator);

        assertThat(playlist.getName()).isEqualTo("재생목록1");
        assertThat(playlist.getDescription()).isEqualTo("설명");
        assertThat(playlist.getIsPublic()).isFalse();
        assertThat(playlist.getCreatedByUser()).isEqualTo(creator);
        assertThat(playlist.getPlaylistVideos()).isEmpty();
        assertThat(playlist.getThumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("재생목록 수정")
    void update() {
        User creator = User.create("admin@test.com", "encoded", "관리자", "관리자");
        Playlist playlist = Playlist.create("재생목록1", "설명", false, creator);

        playlist.update("수정된 이름", "수정된 설명", true);

        assertThat(playlist.getName()).isEqualTo("수정된 이름");
        assertThat(playlist.getDescription()).isEqualTo("수정된 설명");
        assertThat(playlist.getIsPublic()).isTrue();
    }
}
