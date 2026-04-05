package com.videostation.persistence;

import com.videostation.domain.PlaylistVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistVideoRepository extends JpaRepository<PlaylistVideo, Long> {

    List<PlaylistVideo> findByPlaylistIdOrderBySortOrderAsc(Long playlistId);

    Optional<PlaylistVideo> findByPlaylistIdAndVideoId(Long playlistId, Long videoId);

    int countByPlaylistId(Long playlistId);
}
