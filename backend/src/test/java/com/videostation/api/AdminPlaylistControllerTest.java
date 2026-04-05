package com.videostation.api;

import tools.jackson.databind.ObjectMapper;
import com.videostation.application.PlaylistService;
import com.videostation.application.dto.*;
import com.videostation.domain.constant.UserRole;
import com.videostation.global.auth.JwtProvider;
import com.videostation.global.config.SecurityConfig;
import com.videostation.global.filter.TokenAuthenticationFilter;
import com.videostation.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPlaylistController.class)
@Import({SecurityConfig.class, TokenAuthenticationFilter.class})
class AdminPlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlaylistService playlistService;

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

    private PlaylistResponse samplePlaylist() {
        return new PlaylistResponse(1L, "목록1", "설명", null, false, 3, 900, null, LocalDateTime.now());
    }

    private PlaylistDetailResponse sampleDetail() {
        return new PlaylistDetailResponse(1L, "목록1", "설명", null, false, 0, 0, null, LocalDateTime.now(), List.of());
    }

    @Test
    @DisplayName("POST /api/v1/admin/playlists - 생성")
    void create() throws Exception {
        given(playlistService.create(any(PlaylistRequest.class), any())).willReturn(samplePlaylist());

        mockMvc.perform(post("/api/v1/admin/playlists")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlaylistRequest("목록1", "설명", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("목록1"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/playlists - 목록")
    void list() throws Exception {
        var page = new PageImpl<>(List.of(samplePlaylist()), PageRequest.of(0, 20), 1);
        given(playlistService.getAdminPlaylists(any())).willReturn(page);

        mockMvc.perform(get("/api/v1/admin/playlists")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("목록1"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/playlists/{id} - 상세")
    void detail() throws Exception {
        given(playlistService.getPlaylistDetail(1L)).willReturn(sampleDetail());

        mockMvc.perform(get("/api/v1/admin/playlists/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/playlists/{id} - 수정")
    void update() throws Exception {
        given(playlistService.update(eq(1L), any(PlaylistRequest.class))).willReturn(samplePlaylist());

        mockMvc.perform(put("/api/v1/admin/playlists/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlaylistRequest("수정", "설명", true))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/playlists/{id} - 삭제")
    void deletePlaylist() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/playlists/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/admin/playlists/{id}/videos - 영상 추가")
    void addVideo() throws Exception {
        given(playlistService.addVideo(1L, 10L)).willReturn(sampleDetail());

        mockMvc.perform(post("/api/v1/admin/playlists/1/videos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"videoId\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/playlists/{id}/videos/{videoId} - 영상 제거")
    void removeVideo() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/playlists/1/videos/10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/playlists/{id}/videos/reorder - 순서 변경")
    void reorder() throws Exception {
        given(playlistService.reorder(eq(1L), any(ReorderRequest.class))).willReturn(sampleDetail());

        mockMvc.perform(put("/api/v1/admin/playlists/1/videos/reorder")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReorderRequest(List.of(20L, 10L)))))
                .andExpect(status().isOk());
    }
}
