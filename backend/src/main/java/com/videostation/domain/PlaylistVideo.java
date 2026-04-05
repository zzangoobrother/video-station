package com.videostation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlist_videos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistVideo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false)
    private Integer sortOrder;

    private PlaylistVideo(Playlist playlist, Video video, int sortOrder) {
        this.playlist = playlist;
        this.video = video;
        this.sortOrder = sortOrder;
    }

    public static PlaylistVideo create(Playlist playlist, Video video, int sortOrder) {
        return new PlaylistVideo(playlist, video, sortOrder);
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
