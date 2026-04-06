package com.videostation.domain;

import com.videostation.domain.constant.BroadcastStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "broadcasts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Broadcast extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_video_id")
    private Video currentVideo;

    @Column(nullable = false)
    private Integer currentVideoIndex = 0;

    private LocalDateTime currentVideoStartedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BroadcastStatus status = BroadcastStatus.IDLE;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime pausedAt;

    @Column(nullable = false)
    private Long totalPausedSeconds = 0L;

    @Column(nullable = false)
    private Long currentVideoPausedSeconds = 0L;

    @Column(nullable = false)
    private Boolean loopPlaylist = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by", nullable = false)
    private User startedBy;

    private Broadcast(Channel channel, Playlist playlist, Boolean loopPlaylist, User startedBy) {
        this.channel = channel;
        this.playlist = playlist;
        this.loopPlaylist = loopPlaylist;
        this.startedBy = startedBy;
    }

    public static Broadcast create(Channel channel, Playlist playlist, Boolean loopPlaylist, User startedBy) {
        return new Broadcast(channel, playlist, loopPlaylist, startedBy);
    }

    public void start(Video firstVideo) {
        this.status = BroadcastStatus.LIVE;
        this.currentVideo = firstVideo;
        this.currentVideoIndex = 0;
        this.startedAt = LocalDateTime.now();
        this.currentVideoStartedAt = LocalDateTime.now();
    }

    public void pause() {
        this.status = BroadcastStatus.PAUSED;
        this.pausedAt = LocalDateTime.now();
    }

    public void resume() {
        if (this.pausedAt != null) {
            long pausedDuration = java.time.Duration.between(this.pausedAt, LocalDateTime.now()).getSeconds();
            this.totalPausedSeconds += pausedDuration;
            this.currentVideoPausedSeconds += pausedDuration;
        }
        this.status = BroadcastStatus.LIVE;
        this.pausedAt = null;
    }

    public void end() {
        this.status = BroadcastStatus.ENDED;
        this.endedAt = LocalDateTime.now();
        if (this.pausedAt != null) {
            long pausedDuration = java.time.Duration.between(this.pausedAt, LocalDateTime.now()).getSeconds();
            this.totalPausedSeconds += pausedDuration;
            this.pausedAt = null;
        }
    }

    public void switchVideo(Video video, int index) {
        this.currentVideo = video;
        this.currentVideoIndex = index;
        this.currentVideoStartedAt = LocalDateTime.now();
        this.currentVideoPausedSeconds = 0L;
    }

    public long calculateCurrentOffsetSeconds() {
        if (currentVideoStartedAt == null) {
            return 0;
        }
        long elapsed = java.time.Duration.between(currentVideoStartedAt, LocalDateTime.now()).getSeconds();
        // 이전 일시정지 누적 시간 차감
        elapsed -= currentVideoPausedSeconds;
        // 현재 진행 중인 일시정지 시간 차감
        if (status == BroadcastStatus.PAUSED && pausedAt != null) {
            long currentPause = java.time.Duration.between(pausedAt, LocalDateTime.now()).getSeconds();
            elapsed -= currentPause;
        }
        return Math.max(0, elapsed);
    }
}
