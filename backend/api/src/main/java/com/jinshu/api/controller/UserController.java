package com.jinshu.api.controller;

import com.jinshu.api.service.UserService;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.User;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import com.jinshu.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @RequireRole("ADMIN")
    @AuditLog(operation = "CREATE_USER", targetType = "USER")
    public Result<User> createUser(@RequestBody UserService.CreateUserRequest request) {
        User user = userService.createUser(request);
        return Result.success(user);
    }

    @GetMapping
    @RequireRole("ADMIN")
    public Result<PageResult<User>> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<User> result = userService.listUsers(username, role, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    @AuditLog(operation = "UPDATE_USER", targetType = "USER")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody UserService.UpdateUserRequest request) {
        User user = userService.updateUser(id, request);
        return Result.success(user);
    }

    @PutMapping("/{id}/role")
    @RequireRole("ADMIN")
    @AuditLog(operation = "CHANGE_ROLE", targetType = "USER")
    public Result<User> changeUserRole(@PathVariable Long id, @RequestBody UserService.ChangeRoleRequest request) {
        User user = userService.changeRole(id, request.getRole(), request.getReason());
        return Result.success(user);
    }

    @PatchMapping("/{id}/status")
    @RequireRole("ADMIN")
    @AuditLog(operation = "CHANGE_USER_STATUS", targetType = "USER")
    public Result<User> changeUserStatus(@PathVariable Long id, @RequestParam String status) {
        User user = userService.changeUserStatus(id, status);
        return Result.success(user);
    }

    @PostMapping("/{id}/reset-password")
    @RequireRole("ADMIN")
    @AuditLog(operation = "RESET_PASSWORD", targetType = "USER")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success(null);
    }

    @PutMapping("/password")
    @AuditLog(operation = "CHANGE_PASSWORD", targetType = "USER")
    public Result<Void> changePassword(@RequestBody UserService.ChangePasswordRequest request) {
        Long userId = UserContext.getUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success(null);
    }

    @GetMapping("/me")
    public Result<User> getCurrentUser() {
        User user = userService.getCurrentUser();
        return Result.success(user);
    }
}
