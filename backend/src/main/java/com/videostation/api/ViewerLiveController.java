package com.videostation.api;

import com.videostation.application.BroadcastService;
import com.videostation.application.dto.BroadcastStateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/viewer")
@RequiredArgsConstructor
public class ViewerLiveController {

    private final BroadcastService broadcastService;

    @GetMapping("/live")
    public ResponseEntity<BroadcastStateResponse> getLiveBroadcast() {
        BroadcastStateResponse state = broadcastService.getLiveBroadcast();
        if (state == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(state);
    }
}
