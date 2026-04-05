package com.videostation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistVideoTest {

    @Test
    @DisplayName("PlaylistVideo 생성")
    void create() {
        User user = User.create("admin@test.com", "encoded", "관리자", "관리자");
        Playlist playlist = Playlist.create("목록", "설명", false, user);
        Video video = Video.create("영상", "설명", "태그", "/path", "file.mp4", 1024L, user);

        PlaylistVideo pv = PlaylistVideo.create(playlist, video, 1);

        assertThat(pv.getPlaylist()).isEqualTo(playlist);
        assertThat(pv.getVideo()).isEqualTo(video);
        assertThat(pv.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("순서 변경")
    void changeSortOrder() {
        User user = User.create("admin@test.com", "encoded", "관리자", "관리자");
        Playlist playlist = Playlist.create("목록", "설명", false, user);
        Video video = Video.create("영상", "설명", "태그", "/path", "file.mp4", 1024L, user);
        PlaylistVideo pv = PlaylistVideo.create(playlist, video, 1);

        pv.changeSortOrder(3);

        assertThat(pv.getSortOrder()).isEqualTo(3);
    }
}
