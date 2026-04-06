package com.videostation.broadcast;

import com.videostation.domain.Broadcast;
import com.videostation.domain.constant.BroadcastStatus;
import com.videostation.persistence.BroadcastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class BroadcastStateManager {

    private final BroadcastRepository broadcastRepository;
    private final Set<Long> activeBroadcastIds = ConcurrentHashMap.newKeySet();

    @EventListener(ApplicationReadyEvent.class)
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void recoverOnStartup() {
        List<Broadcast> active = broadcastRepository.findByStatusIn(
                List.of(BroadcastStatus.LIVE, BroadcastStatus.PAUSED));
        for (Broadcast broadcast : active) {
            activeBroadcastIds.add(broadcast.getId());
            log.info("방송 상태 복구 - 방송 ID: {}, 채널: {}", broadcast.getId(), broadcast.getChannel().getName());
        }
        if (!active.isEmpty()) {
            log.info("총 {}건의 방송 상태 복구 완료", active.size());
        }
    }

    public void register(Long broadcastId) {
        activeBroadcastIds.add(broadcastId);
    }

    public void unregister(Long broadcastId) {
        activeBroadcastIds.remove(broadcastId);
    }

    public List<Long> getActiveBroadcastIds() {
        return List.copyOf(activeBroadcastIds);
    }
}
