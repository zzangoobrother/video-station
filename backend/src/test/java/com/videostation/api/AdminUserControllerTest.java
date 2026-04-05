package com.videostation.api;

import tools.jackson.databind.ObjectMapper;
import com.videostation.application.UserAdminService;
import com.videostation.application.dto.UserResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, TokenAuthenticationFilter.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserAdminService userAdminService;

    @MockitoBean
    private JwtProvider jwtProvider;

    private String superAdminToken;

    @BeforeEach
    void setUp() {
        superAdminToken = "super-admin-token";
        given(jwtProvider.validateToken(superAdminToken)).willReturn(true);
        given(jwtProvider.getUserId(superAdminToken)).willReturn(1L);
        given(jwtProvider.getEmail(superAdminToken)).willReturn("super@test.com");
        given(jwtProvider.getRole(superAdminToken)).willReturn(UserRole.SUPER_ADMIN);
    }

    private UserResponse sampleUser() {
        return new UserResponse(2L, "user@test.com", "유저", "닉네임", "VIEWER", "ACTIVE", null, LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - 사용자 목록")
    void list() throws Exception {
        var page = new PageImpl<>(List.of(sampleUser()), PageRequest.of(0, 20), 1);
        given(userAdminService.getUsers(any())).willReturn(page);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("user@test.com"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/role - 역할 변경")
    void changeRole() throws Exception {
        var updated = new UserResponse(2L, "user@test.com", "유저", "닉네임", "ADMIN", "ACTIVE", null, LocalDateTime.now());
        given(userAdminService.changeRole(eq(2L), eq(UserRole.ADMIN))).willReturn(updated);

        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/status - 상태 변경")
    void changeStatus() throws Exception {
        var updated = new UserResponse(2L, "user@test.com", "유저", "닉네임", "VIEWER", "BANNED", null, LocalDateTime.now());
        given(userAdminService.changeStatus(eq(2L), any())).willReturn(updated);

        mockMvc.perform(patch("/api/v1/admin/users/2/status")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BANNED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BANNED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/users/{id} - 삭제")
    void deleteUser() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/2")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("ADMIN 권한으로 사용자 관리 접근 시 403")
    void adminForbidden() throws Exception {
        String adminToken = "admin-token";
        given(jwtProvider.validateToken(adminToken)).willReturn(true);
        given(jwtProvider.getUserId(adminToken)).willReturn(2L);
        given(jwtProvider.getEmail(adminToken)).willReturn("admin@test.com");
        given(jwtProvider.getRole(adminToken)).willReturn(UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }
}
