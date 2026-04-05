package com.videostation.persistence;

import com.videostation.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    boolean existsByName(String name);

    Optional<Channel> findByName(String name);
}
