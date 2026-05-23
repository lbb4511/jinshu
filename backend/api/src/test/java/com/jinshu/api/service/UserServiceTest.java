package com.jinshu.api.service;

import com.jinshu.api.dao.UserMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.User;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.security.TokenVersionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("UserService 用户管理服务测试")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenVersionManager tokenVersionManager;

    private UserService userService;

    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, passwordEncoder, tokenVersionManager);
        TenantContext.setTenantId(TENANT_ID);
        UserContext.setUserId(1L);
        UserContext.setUsername("admin");
        UserContext.setRole("ADMIN");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    private User createDefaultUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setTenantId(TENANT_ID);
        user.setUsername("zhangsan");
        user.setDisplayName("张三");
        user.setEmail("zhangsan@example.com");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setPasswordUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    @DisplayName("createUser：创建用户成功")
    void given_validRequest_when_createUser_then_success() {
        when(userMapper.selectByUsernameAndTenantId("zhangsan", TENANT_ID)).thenReturn(null);
        when(passwordEncoder.encode("Password1!")).thenReturn("$2a$10$encoded");
        doAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(USER_ID);
            return null;
        }).when(userMapper).insert(any(User.class));

        UserService.CreateUserRequest request = new UserService.CreateUserRequest();
        request.setUsername("zhangsan");
        request.setPassword("Password1!");
        request.setDisplayName("张三");
        request.setEmail("zhangsan@example.com");
        request.setRole("USER");

        User result = userService.createUser(request);

        assertThat(result.getUsername()).isEqualTo("zhangsan");
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getRole()).isEqualTo("USER");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("createUser：用户名重复抛异常")
    void given_duplicateUsername_when_createUser_then_throw() {
        when(userMapper.selectByUsernameAndTenantId("dup", TENANT_ID)).thenReturn(new User());

        UserService.CreateUserRequest request = new UserService.CreateUserRequest();
        request.setUsername("dup");

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    @DisplayName("createUser：弱密码抛异常")
    void given_weakPassword_when_createUser_then_throw() {
        when(userMapper.selectByUsernameAndTenantId("zhangsan", TENANT_ID)).thenReturn(null);

        UserService.CreateUserRequest request = new UserService.CreateUserRequest();
        request.setUsername("zhangsan");
        request.setPassword("123456");

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码必须至少8位");
    }

    @Test
    @DisplayName("getUserById：相同租户可查询")
    void given_sameTenantUser_when_getById_then_return() {
        when(userMapper.selectById(USER_ID)).thenReturn(createDefaultUser());

        User result = userService.getUserById(USER_ID);

        assertThat(result.getUsername()).isEqualTo("zhangsan");
    }

    @Test
    @DisplayName("getUserById：跨租户不可查询")
    void given_differentTenantUser_when_getById_then_throw() {
        User otherTenantUser = createDefaultUser();
        otherTenantUser.setTenantId(999L);
        when(userMapper.selectById(USER_ID)).thenReturn(otherTenantUser);

        assertThatThrownBy(() -> userService.getUserById(USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
    }

    @Test
    @DisplayName("getUserById：不存在抛异常")
    void given_nonExistentUser_when_getById_then_throw() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.USER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("listUsers：分页查询成功")
    void given_pagination_when_listUsers_then_returnPage() {
        when(userMapper.selectList(eq(TENANT_ID), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(createDefaultUser()));
        when(userMapper.countList(eq(TENANT_ID), any(), any())).thenReturn(1L);

        PageResult<User> result = userService.listUsers(null, null, 1, 20);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateUser：更新用户成功")
    void given_validRequest_when_updateUser_then_success() {
        User existing = createDefaultUser();
        when(userMapper.selectById(USER_ID)).thenReturn(existing);

        UserService.UpdateUserRequest request = new UserService.UpdateUserRequest();
        request.setDisplayName("李四");
        request.setEmail("lisi@example.com");
        request.setRole("ADMIN");

        User result = userService.updateUser(USER_ID, request);

        assertThat(result.getDisplayName()).isEqualTo("李四");
        assertThat(result.getEmail()).isEqualTo("lisi@example.com");
        assertThat(result.getRole()).isEqualTo("ADMIN");
        verify(userMapper).update(any(User.class));
    }

    @Test
    @DisplayName("changePassword：密码修改成功")
    void given_correctOldPassword_when_changePassword_then_success() {
        User existing = createDefaultUser();
        existing.setPasswordHash("$2a$10$old_hash");
        when(userMapper.selectById(USER_ID)).thenReturn(existing);
        when(passwordEncoder.matches("oldPass1!", "$2a$10$old_hash")).thenReturn(true);
        when(passwordEncoder.encode("newPass2@")).thenReturn("$2a$10$new_hash");

        UserService.ChangePasswordRequest request = new UserService.ChangePasswordRequest();
        request.setOldPassword("oldPass1!");
        request.setNewPassword("newPass2@");

        userService.changePassword(USER_ID, request.getOldPassword(), request.getNewPassword());

        verify(userMapper).update(argThat(u ->
                "$2a$10$new_hash".equals(u.getPasswordHash())));
        verify(tokenVersionManager).incrementTokenVersion(USER_ID);
    }

    @Test
    @DisplayName("changePassword：旧密码错误抛异常")
    void given_wrongOldPassword_when_changePassword_then_throw() {
        User existing = createDefaultUser();
        existing.setPasswordHash("$2a$10$old_hash");
        when(userMapper.selectById(USER_ID)).thenReturn(existing);
        when(passwordEncoder.matches("wrong", "$2a$10$old_hash")).thenReturn(false);

        UserService.ChangePasswordRequest request = new UserService.ChangePasswordRequest();
        request.setOldPassword("wrong");
        request.setNewPassword("newPass2@");

        assertThatThrownBy(() -> userService.changePassword(USER_ID, request.getOldPassword(), request.getNewPassword()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码错误");
    }

    @Test
    @DisplayName("disableUser：禁用用户成功")
    void given_existingUser_when_disable_then_statusChanged() {
        User existing = createDefaultUser();
        when(userMapper.selectById(USER_ID)).thenReturn(existing);

        userService.changeUserStatus(USER_ID, "DISABLED");

        verify(userMapper).update(argThat(u -> "DISABLED".equals(u.getStatus())));
        verify(tokenVersionManager).incrementTokenVersion(USER_ID);
    }
}
