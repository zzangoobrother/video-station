package com.videostation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "playlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnailUrl;

    private Boolean isPublic = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdByUser;

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PlaylistVideo> playlistVideos = new ArrayList<>();

    private Playlist(String name, String description, Boolean isPublic, User createdByUser) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
        this.createdByUser = createdByUser;
    }

    public static Playlist create(String name, String description, Boolean isPublic, User createdByUser) {
        return new Playlist(name, description, isPublic, createdByUser);
    }

    public void update(String name, String description, Boolean isPublic) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
    }

    public int getTotalDurationSeconds() {
        return playlistVideos.stream()
                .filter(pv -> pv.getVideo().getDurationSeconds() != null)
                .mapToInt(pv -> pv.getVideo().getDurationSeconds())
                .sum();
    }
}
