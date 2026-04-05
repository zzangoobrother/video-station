package com.videostation.api;

import tools.jackson.databind.ObjectMapper;
import com.videostation.application.VideoService;
import com.videostation.application.dto.VideoResponse;
import com.videostation.application.dto.VideoUpdateRequest;
import com.videostation.domain.constant.UserRole;
import com.videostation.domain.constant.VideoStatus;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import com.videostation.persistence.UserRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminVideoController.class)
@Import({SecurityConfig.class, TokenAuthenticationFilter.class})
class AdminVideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VideoService videoService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserRepository userRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = "admin-token";
        given(jwtProvider.validateToken(adminToken)).willReturn(true);
        given(jwtProvider.getUserId(adminToken)).willReturn(1L);
        given(jwtProvider.getEmail(adminToken)).willReturn("admin@test.com");
        given(jwtProvider.getRole(adminToken)).willReturn(UserRole.ADMIN);
    }

    private VideoResponse sampleVideo() {
        return new VideoResponse(1L, "테스트 영상", "설명", "test.mp4", 1024L,
                null, null, "READY", null, "태그", false, 0L, null, LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /api/v1/admin/videos - 동영상 업로드")
    void upload() throws Exception {
        var file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());
        given(videoService.upload(any(), eq("테스트 영상"), eq("설명"), eq("태그"), any())).willReturn(sampleVideo());

        mockMvc.perform(multipart("/api/v1/admin/videos")
                        .file(file)
                        .param("title", "테스트 영상")
                        .param("description", "설명")
                        .param("tags", "태그")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("테스트 영상"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/videos - 영상 목록")
    void list() throws Exception {
        var page = new PageImpl<>(List.of(sampleVideo()), PageRequest.of(0, 20), 1);
        given(videoService.getAdminVideos(isNull(), isNull(), any())).willReturn(page);

        mockMvc.perform(get("/api/v1/admin/videos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("테스트 영상"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/videos/{id} - 상세 조회")
    void getVideo() throws Exception {
        given(videoService.getVideo(1L)).willReturn(sampleVideo());

        mockMvc.perform(get("/api/v1/admin/videos/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/videos/{id} - 메타데이터 수정")
    void updateVideo() throws Exception {
        var updated = new VideoResponse(1L, "수정됨", "수정 설명", "test.mp4", 1024L,
                null, null, "READY", null, "수정 태그", false, 0L, null, LocalDateTime.now());
        given(videoService.updateVideo(eq(1L), any(VideoUpdateRequest.class))).willReturn(updated);

        mockMvc.perform(put("/api/v1/admin/videos/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VideoUpdateRequest("수정됨", "수정 설명", "수정 태그"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정됨"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/videos/{id} - 삭제")
    void deleteVideo() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/videos/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/videos/{id}/visibility - 공개 전환")
    void toggleVisibility() throws Exception {
        var toggled = new VideoResponse(1L, "영상", "설명", "test.mp4", 1024L,
                null, null, "READY", null, "태그", true, 0L, null, LocalDateTime.now());
        given(videoService.toggleVisibility(1L)).willReturn(toggled);

        mockMvc.perform(patch("/api/v1/admin/videos/1/visibility")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/admin/videos/{id}/encoding-status - 인코딩 상태")
    void encodingStatus() throws Exception {
        var video = new VideoResponse(1L, "영상", "설명", "test.mp4", 1024L,
                null, null, "ENCODING", null, "태그", false, 0L, null, LocalDateTime.now());
        given(videoService.getVideo(1L)).willReturn(video);

        mockMvc.perform(get("/api/v1/admin/videos/1/encoding-status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCODING"));
    }

    @Test
    @DisplayName("VIEWER 권한으로 접근 시 403")
    void viewerForbidden() throws Exception {
        String viewerToken = "viewer-token";
        given(jwtProvider.validateToken(viewerToken)).willReturn(true);
        given(jwtProvider.getUserId(viewerToken)).willReturn(2L);
        given(jwtProvider.getEmail(viewerToken)).willReturn("viewer@test.com");
        given(jwtProvider.getRole(viewerToken)).willReturn(UserRole.VIEWER);

        mockMvc.perform(get("/api/v1/admin/videos")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }
}
