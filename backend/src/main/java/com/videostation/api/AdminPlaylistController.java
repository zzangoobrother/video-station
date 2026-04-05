package com.videostation.api;

import com.videostation.application.PlaylistService;
import com.videostation.application.dto.*;
import com.videostation.global.auth.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/playlists")
@RequiredArgsConstructor
public class AdminPlaylistController {

    private final PlaylistService playlistService;

    @PostMapping
    public ResponseEntity<PlaylistResponse> create(
            @Valid @RequestBody PlaylistRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(playlistService.create(request, principal.getId()));
    }

    @GetMapping
    public ResponseEntity<Page<PlaylistResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(playlistService.getAdminPlaylists(pageable));
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDetailResponse> detail(@PathVariable Long playlistId) {
        return ResponseEntity.ok(playlistService.getPlaylistDetail(playlistId));
    }

    @PutMapping("/{playlistId}")
    public ResponseEntity<PlaylistResponse> update(
            @PathVariable Long playlistId,
            @Valid @RequestBody PlaylistRequest request) {
        return ResponseEntity.ok(playlistService.update(playlistId, request));
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> delete(@PathVariable Long playlistId) {
        playlistService.delete(playlistId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{playlistId}/videos")
    public ResponseEntity<PlaylistDetailResponse> addVideo(
            @PathVariable Long playlistId,
            @Valid @RequestBody AddVideoRequest request) {
        return ResponseEntity.ok(playlistService.addVideo(playlistId, request.videoId()));
    }

    @DeleteMapping("/{playlistId}/videos/{videoId}")
    public ResponseEntity<Void> removeVideo(
            @PathVariable Long playlistId,
            @PathVariable Long videoId) {
        playlistService.removeVideo(playlistId, videoId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{playlistId}/videos/reorder")
    public ResponseEntity<PlaylistDetailResponse> reorder(
            @PathVariable Long playlistId,
            @Valid @RequestBody ReorderRequest request) {
        return ResponseEntity.ok(playlistService.reorder(playlistId, request));
    }

    public record AddVideoRequest(@NotNull Long videoId) {}
}
