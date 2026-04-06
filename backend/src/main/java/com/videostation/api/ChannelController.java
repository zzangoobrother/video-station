package com.videostation.api;

import com.videostation.application.BroadcastService;
import com.videostation.application.ChannelService;
import com.videostation.application.dto.*;
import com.videostation.global.auth.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final BroadcastService broadcastService;

    @PostMapping
    public ResponseEntity<ChannelResponse> create(
            @Valid @RequestBody ChannelRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(channelService.create(request, principal.getId()));
    }

    @GetMapping
    public ResponseEntity<Page<ChannelResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(channelService.getChannels(pageable));
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<ChannelResponse> get(@PathVariable Long channelId) {
        return ResponseEntity.ok(channelService.getChannel(channelId));
    }

    @PostMapping("/{channelId}/broadcast/start")
    public ResponseEntity<BroadcastStateResponse> startBroadcast(
            @PathVariable Long channelId,
            @Valid @RequestBody BroadcastStartRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(broadcastService.start(channelId, request, principal.getId()));
    }

    @PostMapping("/{channelId}/broadcast/stop")
    public ResponseEntity<Void> stopBroadcast(@PathVariable Long channelId) {
        broadcastService.stop(channelId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{channelId}/broadcast/pause")
    public ResponseEntity<BroadcastStateResponse> pauseBroadcast(@PathVariable Long channelId) {
        return ResponseEntity.ok(broadcastService.pause(channelId));
    }

    @PostMapping("/{channelId}/broadcast/resume")
    public ResponseEntity<BroadcastStateResponse> resumeBroadcast(@PathVariable Long channelId) {
        return ResponseEntity.ok(broadcastService.resume(channelId));
    }

    @PostMapping("/{channelId}/broadcast/next")
    public ResponseEntity<BroadcastStateResponse> nextVideo(@PathVariable Long channelId) {
        return ResponseEntity.ok(broadcastService.next(channelId));
    }

    @PostMapping("/{channelId}/broadcast/previous")
    public ResponseEntity<BroadcastStateResponse> previousVideo(@PathVariable Long channelId) {
        return ResponseEntity.ok(broadcastService.previous(channelId));
    }

    @PostMapping("/{channelId}/broadcast/jump/{index}")
    public ResponseEntity<BroadcastStateResponse> jumpToVideo(
            @PathVariable Long channelId, @PathVariable int index) {
        return ResponseEntity.ok(broadcastService.jump(channelId, index));
    }

    @GetMapping("/{channelId}/broadcast/state")
    public ResponseEntity<BroadcastStateResponse> getBroadcastState(@PathVariable Long channelId) {
        return ResponseEntity.ok(broadcastService.getState(channelId));
    }
}
