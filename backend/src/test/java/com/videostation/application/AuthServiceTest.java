package com.videostation.application;

import com.videostation.application.dto.LoginRequest;
import com.videostation.application.dto.RegisterRequest;
import com.videostation.application.dto.TokenResponse;
import com.videostation.application.dto.UserResponse;
import com.videostation.domain.User;
import com.videostation.domain.constant.UserRole;
import com.videostation.domain.constant.UserStatus;
import com.videostation.global.auth.JwtProvider;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("회원가입 성공")
    void registerSuccess() {
        var request = new RegisterRequest("test@test.com", "password123", "홍길동", "길동이");
        var savedUser = User.builder()
                .id(1L).email("test@test.com").password("encoded").name("홍길동").nickname("길동이")
                .role(UserRole.VIEWER).status(UserStatus.ACTIVE).build();

        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        UserResponse response = authService.register(request);

        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.role()).isEqualTo("VIEWER");
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void registerDuplicateEmail() {
        var request = new RegisterRequest("dup@test.com", "password123", "홍길동", "길동이");
        given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL));
    }

    @Test
    @DisplayName("로그인 성공")
    void loginSuccess() {
        var request = new LoginRequest("test@test.com", "password123");
        var user = User.builder()
                .id(1L).email("test@test.com").password("encoded").name("홍길동").nickname("길동이")
                .role(UserRole.VIEWER).status(UserStatus.ACTIVE).build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encoded")).willReturn(true);
        given(jwtProvider.createAccessToken(1L, "test@test.com", UserRole.VIEWER)).willReturn("access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token");

        AuthService.LoginResult result = authService.login(request);

        assertThat(result.tokenResponse().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void loginWrongPassword() {
        var request = new LoginRequest("test@test.com", "wrong");
        var user = User.builder()
                .id(1L).email("test@test.com").password("encoded").name("홍길동").nickname("길동이")
                .role(UserRole.VIEWER).status(UserStatus.ACTIVE).build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    @DisplayName("로그인 실패 - 비활성 계정")
    void loginDisabledAccount() {
        var request = new LoginRequest("test@test.com", "password123");
        var user = User.builder()
                .id(1L).email("test@test.com").password("encoded").name("홍길동").nickname("길동이")
                .role(UserRole.VIEWER).status(UserStatus.BANNED).build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encoded")).willReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_DISABLED));
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshSuccess() {
        var user = User.builder()
                .id(1L).email("test@test.com").password("encoded").name("홍길동").nickname("길동이")
                .role(UserRole.VIEWER).status(UserStatus.ACTIVE).build();

        given(jwtProvider.validateToken("refresh-token")).willReturn(true);
        given(jwtProvider.getUserId("refresh-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(1L, "test@test.com", UserRole.VIEWER)).willReturn("new-access-token");

        TokenResponse response = authService.refresh("refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰")
    void refreshInvalidToken() {
        given(jwtProvider.validateToken("invalid")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh("invalid"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }
}
