package com.videostation.application;

import com.videostation.application.dto.UserResponse;
import com.videostation.domain.User;
import com.videostation.domain.constant.UserRole;
import com.videostation.domain.constant.UserStatus;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @InjectMocks
    private UserAdminService userAdminService;

    @Mock
    private UserRepository userRepository;

    private User createUser(Long id, String email, UserRole role) {
        User user = User.create(email, "encoded", "이름", "닉네임");
        ReflectionTestUtils.setField(user, "id", id);
        if (role != UserRole.VIEWER) user.changeRole(role);
        return user;
    }

    @Test
    @DisplayName("사용자 목록 조회")
    void getUsers() {
        var user = createUser(1L, "test@test.com", UserRole.VIEWER);
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
        given(userRepository.findAll(any(PageRequest.class))).willReturn(page);

        var result = userAdminService.getUsers(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("역할 변경")
    void changeRole() {
        var user = createUser(1L, "test@test.com", UserRole.VIEWER);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userAdminService.changeRole(1L, UserRole.ADMIN);

        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("상태 변경")
    void changeStatus() {
        var user = createUser(1L, "test@test.com", UserRole.VIEWER);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userAdminService.changeStatus(1L, UserStatus.BANNED);

        assertThat(response.status()).isEqualTo("BANNED");
    }

    @Test
    @DisplayName("사용자 삭제")
    void deleteUser() {
        var user = createUser(1L, "test@test.com", UserRole.VIEWER);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userAdminService.deleteUser(1L);

        then(userRepository).should().delete(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 역할 변경 시 예외")
    void changeRoleNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.changeRole(999L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
