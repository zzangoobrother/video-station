package com.videostation.api;

import com.videostation.application.ViewerService;
import com.videostation.application.dto.VideoResponse;
import com.videostation.domain.constant.UserRole;
import com.videostation.global.auth.JwtProvider;
import com.videostation.global.config.SecurityConfig;
import com.videostation.global.filter.TokenAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ViewerVideoController.class)
@Import({SecurityConfig.class, TokenAuthenticationFilter.class})
class ViewerVideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ViewerService viewerService;

    @MockitoBean
    private JwtProvider jwtProvider;

    private String viewerToken;

    @BeforeEach
    void setUp() {
        viewerToken = "viewer-token";
        given(jwtProvider.validateToken(viewerToken)).willReturn(true);
        given(jwtProvider.getUserId(viewerToken)).willReturn(2L);
        given(jwtProvider.getEmail(viewerToken)).willReturn("viewer@test.com");
        given(jwtProvider.getRole(viewerToken)).willReturn(UserRole.VIEWER);
    }

    private VideoResponse sampleVideo() {
        return new VideoResponse(1L, "영상1", "설명", "test.mp4", 1024L,
                "/thumbnails/1.jpg", 300, "READY", "/hls/1/master.m3u8", "태그",
                true, 10L, null, LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/v1/videos - 공개 영상 목록")
    void list() throws Exception {
        var page = new PageImpl<>(List.of(sampleVideo()), PageRequest.of(0, 20), 1);
        given(viewerService.getPublicVideos(isNull(), any())).willReturn(page);

        mockMvc.perform(get("/api/v1/videos")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("영상1"));
    }

    @Test
    @DisplayName("GET /api/v1/videos/{id} - 영상 재생 정보")
    void get_() throws Exception {
        given(viewerService.getVideoForPlay(1L)).willReturn(sampleVideo());

        mockMvc.perform(get("/api/v1/videos/1")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hlsUrl").value("/hls/1/master.m3u8"));
    }

    @Test
    @DisplayName("인증 없으면 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/videos"))
                .andExpect(status().isUnauthorized());
    }
}
