package com.videostation.application;

import com.videostation.application.dto.UserResponse;
import com.videostation.domain.User;
import com.videostation.domain.constant.UserRole;
import com.videostation.domain.constant.UserStatus;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminService {

    private final UserRepository userRepository;

    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse changeRole(Long userId, UserRole role) {
        User user = findUser(userId);
        user.changeRole(role);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changeStatus(Long userId, UserStatus status) {
        User user = findUser(userId);
        user.changeStatus(status);
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findUser(userId);
        userRepository.delete(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
