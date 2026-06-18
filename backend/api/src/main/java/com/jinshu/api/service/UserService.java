package com.jinshu.api.service;

import com.jinshu.api.dao.RoleChangeLogMapper;
import com.jinshu.api.dao.UserMapper;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.RoleChangeLog;
import com.jinshu.common.entity.User;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.security.Role;
import com.jinshu.common.security.TokenVersionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleChangeLogMapper roleChangeLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenVersionManager tokenVersionManager;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$"
    );

    @Transactional
    @AuditLog(operation = "CREATE_USER", targetType = "USER")
    public User createUser(CreateUserRequest request) {
        Long tenantId = TenantContext.getTenantId();

        User existing = userMapper.selectByUsernameAndTenantId(request.getUsername(), tenantId);
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }

        if (!isValidPassword(request.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "密码必须至少8位，包含大写字母、小写字母、数字和特殊字符");
        }

        if (!Role.isValid(request.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的角色: " + request.getRole());
        }

        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setStatus("ACTIVE");
        user.setLoginFailCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setPasswordUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    public User getUserById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!user.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
        return user;
    }

    public PageResult<User> listUsers(String username, String role, int page, int pageSize) {
        Long tenantId = TenantContext.getTenantId();
        int offset = (page - 1) * pageSize;
        List<User> users = userMapper.selectList(tenantId, username, role, offset, pageSize);
        long total = userMapper.countList(tenantId, username, role);
        return PageResult.of(users, total, page, pageSize);
    }

    @Transactional
    @AuditLog(operation = "UPDATE_USER", targetType = "USER")
    public User updateUser(Long id, UpdateUserRequest request) {
        User user = getUserById(id);

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.update(user);
        return user;
    }

    /**
     * 变更用户角色
     *
     * <p>仅允许 ADMIN 调用。角色变更会记录到 sys.role_change_log 审计表，
     * 并递增目标用户的 Token 版本，使其旧 Token 失效。</p>
     *
     * @param id     目标用户ID
     * @param role   新角色
     * @param reason 变更原因
     * @return 更新后的用户
     */
    @Transactional
    @AuditLog(operation = "CHANGE_ROLE", targetType = "USER")
    public User changeRole(Long id, String role, String reason) {
        User user = getUserById(id);
        Long currentUserId = UserContext.getUserId();

        if (id.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能修改自己的角色");
        }

        if (!Role.isValid(role)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的角色: " + role);
        }

        String oldRole = user.getRole();
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);

        RoleChangeLog changeLog = new RoleChangeLog();
        changeLog.setTenantId(user.getTenantId());
        changeLog.setTargetUserId(id);
        changeLog.setOperatorUserId(currentUserId);
        changeLog.setOldRole(oldRole);
        changeLog.setNewRole(role);
        changeLog.setReason(reason);
        changeLog.setCreatedAt(LocalDateTime.now());
        roleChangeLogMapper.insert(changeLog);

        tokenVersionManager.incrementTokenVersion(id);

        return user;
    }

    @Transactional
    @AuditLog(operation = "CHANGE_USER_STATUS", targetType = "USER")
    public User changeUserStatus(Long id, String status) {
        User user = getUserById(id);
        Long currentUserId = UserContext.getUserId();

        if (id.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能操作自己的账号");
        }

        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的用户状态");
        }

        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);

        if ("DISABLED".equals(status)) {
            tokenVersionManager.incrementTokenVersion(id);
        }

        return user;
    }

    @Transactional
    @AuditLog(operation = "RESET_PASSWORD", targetType = "USER")
    public void resetPassword(Long id, String newPassword) {
        User user = getUserById(id);

        if (!isValidPassword(newPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "密码必须至少8位，包含大写字母、小写字母、数字和特殊字符");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);

        tokenVersionManager.incrementTokenVersion(id);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "旧密码错误");
        }

        if (!isValidPassword(newPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "密码必须至少8位，包含大写字母、小写字母、数字和特殊字符");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);

        tokenVersionManager.incrementTokenVersion(userId);
    }

    public User getCurrentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static class CreateUserRequest {
        private String username;
        private String password;
        private String displayName;
        private String email;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class UpdateUserRequest {
        private String displayName;
        private String email;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ChangeRoleRequest {
        private String role;
        private String reason;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
