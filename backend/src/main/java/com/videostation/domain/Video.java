package com.videostation.domain;

import com.videostation.domain.constant.VideoStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "videos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Video extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String originalFilePath;

    private String originalFileName;

    private Long fileSize;

    private String thumbnailPath;

    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VideoStatus status = VideoStatus.UPLOADING;

    private String hlsPath;

    private String objectStorageKey;

    private String tags;

    @Builder.Default
    private Boolean isPublic = false;

    @Builder.Default
    private Long viewCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    public void updateMetadata(String title, String description, String tags) {
        this.title = title;
        this.description = description;
        this.tags = tags;
    }

    public void changeStatus(VideoStatus status) {
        this.status = status;
    }

    public void toggleVisibility() {
        this.isPublic = !this.isPublic;
    }

    public void completeEncoding(String hlsPath, Integer durationSeconds, String thumbnailPath) {
        this.hlsPath = hlsPath;
        this.durationSeconds = durationSeconds;
        this.thumbnailPath = thumbnailPath;
        this.status = VideoStatus.READY;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void setObjectStorageKey(String key) {
        this.objectStorageKey = key;
    }
}
