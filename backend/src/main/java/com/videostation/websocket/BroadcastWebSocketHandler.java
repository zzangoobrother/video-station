package com.videostation.websocket;

import com.videostation.application.dto.BroadcastStateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class BroadcastWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, Lock> sessionLocks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final AtomicReference<BroadcastStateResponse> latestState = new AtomicReference<>();

    private Lock getLock(WebSocketSession session) {
        return sessionLocks.computeIfAbsent(session.getId(), id -> new ReentrantLock());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.debug("WebSocket 연결 - 세션: {}, 현재 연결 수: {}", session.getId(), sessions.size());

        // 연결 시 최신 상태 즉시 전송
        BroadcastStateResponse current = latestState.get();
        if (current != null) {
            Lock lock = getLock(session);
            lock.lock();
            try {
                String json = objectMapper.writeValueAsString(current);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.warn("WebSocket 초기 상태 전송 실패 - 세션: {}", session.getId(), e);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        sessionLocks.remove(session.getId());
        log.debug("WebSocket 해제 - 세션: {}, 현재 연결 수: {}", session.getId(), sessions.size());
    }

    public void broadcastState(BroadcastStateResponse state) {
        latestState.set(state);
        try {
            String json = objectMapper.writeValueAsString(state);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (!session.isOpen()) {
                    sessions.remove(session);
                    sessionLocks.remove(session.getId());
                    continue;
                }
                Lock lock = getLock(session);
                lock.lock();
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.warn("WebSocket 메시지 전송 실패 - 세션: {}", session.getId(), e);
                    sessions.remove(session);
                    sessionLocks.remove(session.getId());
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("WebSocket 브로드캐스트 실패", e);
        }
    }
}
