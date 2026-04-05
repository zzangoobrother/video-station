package com.videostation.persistence;

import com.videostation.domain.Broadcast;
import com.videostation.domain.constant.BroadcastStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {

    Optional<Broadcast> findByChannelIdAndStatusIn(Long channelId, List<BroadcastStatus> statuses);

    List<Broadcast> findByStatusIn(List<BroadcastStatus> statuses);

    @Query("SELECT b FROM Broadcast b " +
           "JOIN FETCH b.playlist p " +
           "LEFT JOIN FETCH p.playlistVideos pv " +
           "LEFT JOIN FETCH pv.video " +
           "JOIN FETCH b.channel " +
           "LEFT JOIN FETCH b.currentVideo " +
           "WHERE b.id IN :ids AND b.status = 'LIVE'")
    List<Broadcast> findActiveWithDetails(@Param("ids") Collection<Long> ids);
}
