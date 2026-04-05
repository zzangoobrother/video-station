package com.videostation.api;

import com.videostation.application.ViewerService;
import com.videostation.application.dto.PlaylistDetailResponse;
import com.videostation.application.dto.PlaylistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
public class ViewerPlaylistController {

    private final ViewerService viewerService;

    @GetMapping
    public ResponseEntity<Page<PlaylistResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(viewerService.getPublicPlaylists(pageable));
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDetailResponse> get(@PathVariable Long playlistId) {
        return ResponseEntity.ok(viewerService.getPublicPlaylistDetail(playlistId));
    }
}
