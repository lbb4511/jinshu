package com.jinshu.api.integration;

import com.jinshu.api.dao.UserMapper;
import com.jinshu.api.service.UserService;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.User;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserService - DB integration tests")
class UserServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Nested
    @DisplayName("Create user")
    @Transactional
    class Create {

        @Test
        @DisplayName("Should create user with valid data")
        void shouldCreateUser() {
            var request = new UserService.CreateUserRequest();
            request.setUsername("newuser");
            request.setPassword("Test@1234");
            request.setDisplayName("新用户");
            request.setEmail("new@test.com");
            request.setRole("USER");

            User user = userService.createUser(request);

            assertThat(user.getId()).isNotNull();
            assertThat(user.getUsername()).isEqualTo("newuser");
            assertThat(user.getPasswordHash()).startsWith("$2a$");
            assertThat(user.getRole()).isEqualTo("USER");
            assertThat(user.getStatus()).isEqualTo("ACTIVE");
            assertThat(user.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should reject duplicate username within same tenant")
        void shouldRejectDuplicateUsername() {
            var first = new UserService.CreateUserRequest();
            first.setUsername("duplicate");
            first.setPassword("Test@1234");
            first.setRole("USER");
            userService.createUser(first);

            var second = new UserService.CreateUserRequest();
            second.setUsername("duplicate");
            second.setPassword("Test@1234");
            second.setRole("USER");
            assertThatThrownBy(() -> userService.createUser(second))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should reject weak password")
        void shouldRejectWeakPassword() {
            var request = new UserService.CreateUserRequest();
            request.setUsername("weakpw");
            request.setPassword("123");
            request.setRole("USER");
            assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Get user")
    @Transactional
    class Get {

        private User saved;

        @BeforeEach
        void setUp() {
            var req = new UserService.CreateUserRequest();
            req.setUsername("getuser");
            req.setPassword("Test@1234");
            req.setRole("USER");
            saved = userService.createUser(req);
        }

        @Test
        @DisplayName("Should find user by id")
        void shouldGetById() {
            User user = userService.getUserById(saved.getId());
            assertThat(user.getUsername()).isEqualTo("getuser");
        }

        @Test
        @DisplayName("Should throw when user belongs to different tenant")
        void shouldThrowCrossTenant() {
            TenantContext.setTenantId(999L);
            assertThatThrownBy(() -> userService.getUserById(saved.getId()))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenNotFound() {
            assertThatThrownBy(() -> userService.getUserById(99999L))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("List users")
    @Transactional
    class ListUsers {

        @BeforeEach
        void setUp() {
            for (int i = 0; i < 3; i++) {
                var req = new UserService.CreateUserRequest();
                req.setUsername("listuser" + i);
                req.setPassword("Test@1234");
                req.setRole(i == 0 ? "ADMIN" : "USER");
                userService.createUser(req);
            }
        }

        @Test
        @DisplayName("Should list users with pagination")
        void shouldListPaginated() {
            PageResult<User> result = userService.listUsers(null, null, 1, 2);
            assertThat(result.getList()).hasSize(2);
            assertThat(result.getTotal()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should filter by role")
        void shouldFilterByRole() {
            PageResult<User> result = userService.listUsers(null, "ADMIN", 1, 10);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Should filter by username")
        void shouldFilterByUsername() {
            PageResult<User> result = userService.listUsers("listuser1", null, 1, 10);
            assertThat(result.getList()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Update user")
    @Transactional
    class Update {

        private User saved;

        @BeforeEach
        void setUp() {
            var req = new UserService.CreateUserRequest();
            req.setUsername("updateuser");
            req.setPassword("Test@1234");
            req.setDisplayName("旧名字");
            req.setRole("USER");
            saved = userService.createUser(req);
        }

        @Test
        @DisplayName("Should update display name and email")
        void shouldUpdateProfile() {
            var updateReq = new UserService.UpdateUserRequest();
            updateReq.setDisplayName("新名字");
            updateReq.setEmail("new@test.com");

            User updated = userService.updateUser(saved.getId(), updateReq);
            assertThat(updated.getDisplayName()).isEqualTo("新名字");
            assertThat(updated.getEmail()).isEqualTo("new@test.com");
        }

        @Test
        @DisplayName("Should update role")
        void shouldUpdateRole() {
            var updateReq = new UserService.UpdateUserRequest();
            updateReq.setRole("ADMIN");

            User updated = userService.updateUser(saved.getId(), updateReq);
            assertThat(updated.getRole()).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("Status management")
    @Transactional
    class Status {

        private User saved;

        @BeforeEach
        void setUp() {
            var req = new UserService.CreateUserRequest();
            req.setUsername("statususer");
            req.setPassword("Test@1234");
            req.setRole("USER");
            saved = userService.createUser(req);
        }

        @Test
        @DisplayName("Should disable user")
        void shouldDisable() {
            User disabled = userService.changeUserStatus(saved.getId(), "DISABLED");
            assertThat(disabled.getStatus()).isEqualTo("DISABLED");
        }

        @Test
        @DisplayName("Should not disable self")
        void shouldNotDisableSelf() {
            UserContext.setUserId(saved.getId());
            assertThatThrownBy(() -> userService.changeUserStatus(saved.getId(), "DISABLED"))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should reject invalid status")
        void shouldRejectInvalidStatus() {
            assertThatThrownBy(() -> userService.changeUserStatus(saved.getId(), "INVALID"))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Password management")
    @Transactional
    class Password {

        private User saved;

        @BeforeEach
        void setUp() {
            var req = new UserService.CreateUserRequest();
            req.setUsername("passuser");
            req.setPassword("Old@1234");
            req.setRole("USER");
            saved = userService.createUser(req);
        }

        @Test
        @DisplayName("Should reset password")
        void shouldResetPassword() {
            userService.resetPassword(saved.getId(), "New@1234");
            User user = userMapper.selectById(saved.getId());
            assertThat(user.getPasswordHash()).startsWith("$2a$");
        }

        @Test
        @DisplayName("Should change password with old password")
        void shouldChangePassword() {
            UserContext.setUserId(saved.getId());
            userService.changePassword(saved.getId(), "Old@1234", "New@1234");
            User user = userMapper.selectById(saved.getId());
            assertThat(user.getPasswordHash()).startsWith("$2a$");
        }

        @Test
        @DisplayName("Should reject wrong old password")
        void shouldRejectWrongOldPassword() {
            assertThatThrownBy(() ->
                userService.changePassword(saved.getId(), "WrongPass@1", "New@1234"))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should reject weak new password")
        void shouldRejectWeakPassword() {
            assertThatThrownBy(() -> userService.resetPassword(saved.getId(), "short"))
                .isInstanceOf(BusinessException.class);
        }
    }
}
