package com.videostation.broadcast;

import com.videostation.application.BroadcastService;
import com.videostation.persistence.BroadcastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BroadcastScheduler {

    private final BroadcastStateManager stateManager;
    private final BroadcastRepository broadcastRepository;
    private final BroadcastService broadcastService;

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void checkAndAdvance() {
        List<Long> activeIds = stateManager.getActiveBroadcastIds();
        if (activeIds.isEmpty()) return;

        broadcastRepository.findActiveWithDetails(activeIds).forEach(broadcastService::advanceIfEnded);
    }
}
