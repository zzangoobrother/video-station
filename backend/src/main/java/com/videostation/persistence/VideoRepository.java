package com.videostation.persistence;

import com.videostation.domain.Video;
import com.videostation.domain.constant.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {

    Page<Video> findByStatus(VideoStatus status, Pageable pageable);

    Page<Video> findByIsPublicTrueAndStatus(VideoStatus status, Pageable pageable);

    Page<Video> findByIsPublicTrueAndStatusAndTitleContaining(VideoStatus status, String keyword, Pageable pageable);

    Page<Video> findByTitleContaining(String keyword, Pageable pageable);
}
