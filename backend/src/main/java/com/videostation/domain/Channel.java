package com.videostation.domain;

import com.videostation.domain.constant.ChannelStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelStatus status = ChannelStatus.IDLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private Channel(String name, String description, User owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
    }

    public static Channel create(String name, String description, User owner) {
        return new Channel(name, description, owner);
    }

    public void changeStatus(ChannelStatus status) {
        this.status = status;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
