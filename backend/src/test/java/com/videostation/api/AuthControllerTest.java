package com.videostation.api;

import tools.jackson.databind.ObjectMapper;
import com.videostation.application.AuthService;
import com.videostation.application.dto.*;
import com.videostation.global.auth.JwtProvider;
import com.videostation.global.config.SecurityConfig;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.global.filter.TokenAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, TokenAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("POST /api/v1/auth/register - 회원가입 성공")
    void registerSuccess() throws Exception {
        var request = new RegisterRequest("test@test.com", "password123", "홍길동", "길동이");
        var response = new UserResponse(1L, "test@test.com", "홍길동", "길동이", "VIEWER", "ACTIVE", null, LocalDateTime.now());

        given(authService.register(any(RegisterRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - 이메일 중복 409")
    void registerDuplicate() throws Exception {
        var request = new RegisterRequest("dup@test.com", "password123", "홍길동", "길동이");

        given(authService.register(any(RegisterRequest.class)))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - 유효성 검사 실패 400")
    void registerValidationFail() throws Exception {
        var request = new RegisterRequest("invalid-email", "short", "", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - 로그인 성공")
    void loginSuccess() throws Exception {
        var request = new LoginRequest("test@test.com", "password123");
        var loginResult = new AuthService.LoginResult(
                new TokenResponse("access-token", 1800),
                "refresh-token"
        );

        given(authService.login(any(LoginRequest.class))).willReturn(loginResult);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - 인증 실패 401")
    void loginFail() throws Exception {
        var request = new LoginRequest("test@test.com", "wrong");

        given(authService.login(any(LoginRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - 토큰 갱신 성공")
    void refreshSuccess() throws Exception {
        given(authService.refresh("refresh-token"))
                .willReturn(new TokenResponse("new-access-token", 1800));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - 쿠키 없으면 401")
    void refreshNoCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - 인증 없으면 401")
    void meUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
