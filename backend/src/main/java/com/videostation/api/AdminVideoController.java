package com.videostation.api;

import com.videostation.application.VideoService;
import com.videostation.application.dto.VideoResponse;
import com.videostation.application.dto.VideoUpdateRequest;
import com.videostation.domain.constant.VideoStatus;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/videos")
@RequiredArgsConstructor
public class AdminVideoController {

    private final VideoService videoService;

    @PostMapping
    public ResponseEntity<VideoResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(videoService.upload(file, title, description, tags, principal.getId()));
    }

    @GetMapping
    public ResponseEntity<Page<VideoResponse>> list(
            @RequestParam(value = "status", required = false) VideoStatus status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(videoService.getAdminVideos(status, keyword, pageable));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long videoId) {
        return ResponseEntity.ok(videoService.getVideo(videoId));
    }

    @PutMapping("/{videoId}")
    public ResponseEntity<VideoResponse> updateVideo(
            @PathVariable Long videoId,
            @Valid @RequestBody VideoUpdateRequest request) {
        return ResponseEntity.ok(videoService.updateVideo(videoId, request));
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long videoId) {
        videoService.deleteVideo(videoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{videoId}/visibility")
    public ResponseEntity<VideoResponse> toggleVisibility(@PathVariable Long videoId) {
        return ResponseEntity.ok(videoService.toggleVisibility(videoId));
    }

    @GetMapping("/{videoId}/encoding-status")
    public ResponseEntity<VideoResponse> encodingStatus(@PathVariable Long videoId) {
        return ResponseEntity.ok(videoService.getVideo(videoId));
    }
}
