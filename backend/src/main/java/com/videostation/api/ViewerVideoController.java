package com.videostation.api;

import com.videostation.application.ViewerService;
import com.videostation.application.dto.VideoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class ViewerVideoController {

    private final ViewerService viewerService;

    @GetMapping
    public ResponseEntity<Page<VideoResponse>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(viewerService.getPublicVideos(keyword, pageable));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<VideoResponse> get(@PathVariable Long videoId) {
        return ResponseEntity.ok(viewerService.getVideoForPlay(videoId));
    }
}
