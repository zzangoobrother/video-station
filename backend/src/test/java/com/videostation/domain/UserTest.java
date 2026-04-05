package com.videostation.domain;

import com.videostation.domain.constant.UserRole;
import com.videostation.domain.constant.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("User 생성 시 기본값 확인 - role=VIEWER, status=ACTIVE")
    void createWithDefaults() {
        User user = User.create("test@test.com", "encoded", "홍길동", "길동이");

        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getNickname()).isEqualTo("길동이");
        assertThat(user.getRole()).isEqualTo(UserRole.VIEWER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("역할 변경")
    void changeRole() {
        User user = User.create("test@test.com", "encoded", "홍길동", "길동이");

        user.changeRole(UserRole.ADMIN);

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("상태 변경")
    void changeStatus() {
        User user = User.create("test@test.com", "encoded", "홍길동", "길동이");

        user.changeStatus(UserStatus.BANNED);

        assertThat(user.getStatus()).isEqualTo(UserStatus.BANNED);
    }
}
