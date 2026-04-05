package com.videostation.global.auth;

import com.videostation.domain.constant.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        // Base64 인코딩된 256비트 이상 시크릿
        String secret = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhMjU2";
        long accessTokenExpiry = 1800000;  // 30분
        long refreshTokenExpiry = 604800000; // 7일
        jwtProvider = new JwtProvider(secret, accessTokenExpiry, refreshTokenExpiry);
    }

    @Test
    @DisplayName("Access Token 생성 및 검증")
    void createAndValidateAccessToken() {
        String token = jwtProvider.createAccessToken(1L, "test@test.com", UserRole.VIEWER);

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(1L);
        assertThat(jwtProvider.getEmail(token)).isEqualTo("test@test.com");
        assertThat(jwtProvider.getRole(token)).isEqualTo(UserRole.VIEWER);
    }

    @Test
    @DisplayName("Refresh Token 생성 및 검증")
    void createAndValidateRefreshToken() {
        String token = jwtProvider.createRefreshToken(1L);

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("잘못된 토큰은 검증 실패")
    void invalidToken() {
        assertThat(jwtProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 검증 실패")
    void expiredToken() {
        JwtProvider shortLived = new JwtProvider(
                "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhMjU2",
                0, 0 // 즉시 만료
        );
        String token = shortLived.createAccessToken(1L, "test@test.com", UserRole.VIEWER);

        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("ADMIN 역할 토큰 생성")
    void adminRoleToken() {
        String token = jwtProvider.createAccessToken(2L, "admin@test.com", UserRole.ADMIN);

        assertThat(jwtProvider.getRole(token)).isEqualTo(UserRole.ADMIN);
    }
}
