package com.videostation.api;

import com.videostation.application.UserAdminService;
import com.videostation.application.dto.UserResponse;
import com.videostation.domain.constant.UserRole;
import com.videostation.domain.constant.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userAdminService.getUsers(pageable));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(userAdminService.changeRole(userId, request.role()));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ResponseEntity.ok(userAdminService.changeStatus(userId, request.status()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userAdminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    public record ChangeRoleRequest(@NotNull UserRole role) {}
    public record ChangeStatusRequest(@NotNull UserStatus status) {}
}
