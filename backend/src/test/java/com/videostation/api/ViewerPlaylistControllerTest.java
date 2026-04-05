package com.videostation.api;

import com.videostation.application.ViewerService;
import com.videostation.application.dto.PlaylistDetailResponse;
import com.videostation.application.dto.PlaylistResponse;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ViewerPlaylistController.class)
@Import({SecurityConfig.class, TokenAuthenticationFilter.class})
class ViewerPlaylistControllerTest {

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

    @Test
    @DisplayName("GET /api/v1/playlists - 공개 재생목록 목록")
    void list() throws Exception {
        var pl = new PlaylistResponse(1L, "목록1", "설명", null, true, 3, 900, null, LocalDateTime.now());
        var page = new PageImpl<>(List.of(pl), PageRequest.of(0, 20), 1);
        given(viewerService.getPublicPlaylists(any())).willReturn(page);

        mockMvc.perform(get("/api/v1/playlists")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("목록1"));
    }

    @Test
    @DisplayName("GET /api/v1/playlists/{id} - 재생목록 상세")
    void get_() throws Exception {
        var detail = new PlaylistDetailResponse(1L, "목록1", "설명", null, true, 0, 0, null, LocalDateTime.now(), List.of());
        given(viewerService.getPublicPlaylistDetail(1L)).willReturn(detail);

        mockMvc.perform(get("/api/v1/playlists/1")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
