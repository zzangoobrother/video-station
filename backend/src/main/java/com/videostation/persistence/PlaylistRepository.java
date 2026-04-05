package com.videostation.persistence;

import com.videostation.domain.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    Page<Playlist> findByIsPublicTrue(Pageable pageable);
}
