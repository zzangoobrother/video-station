package com.videostation.application;

import com.videostation.application.dto.LoginRequest;
import com.videostation.application.dto.RegisterRequest;
import com.videostation.application.dto.TokenResponse;
import com.videostation.application.dto.UserResponse;
import com.videostation.domain.User;
import com.videostation.domain.constant.UserStatus;
import com.videostation.global.auth.JwtProvider;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    private static final int ACCESS_TOKEN_EXPIRY_SECONDS = 1800;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .nickname(request.nickname())
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        return new LoginResult(
                new TokenResponse(accessToken, ACCESS_TOKEN_EXPIRY_SECONDS),
                refreshToken
        );
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new TokenResponse(accessToken, ACCESS_TOKEN_EXPIRY_SECONDS);
    }

    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    public record LoginResult(TokenResponse tokenResponse, String refreshToken) {}
}
