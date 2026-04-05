package com.videostation.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_001", "이미 사용 중인 이메일입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "이메일 또는 비밀번호가 올바르지 않습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 토큰입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_004", "사용자를 찾을 수 없습니다"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "AUTH_005", "비활성화된 계정입니다"),

    // 동영상
    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "VIDEO_001", "동영상을 찾을 수 없습니다"),

    // 재생목록
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAYLIST_001", "재생목록을 찾을 수 없습니다"),

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류입니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
