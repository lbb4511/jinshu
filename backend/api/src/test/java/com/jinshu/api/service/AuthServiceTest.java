package com.jinshu.api.service;

import com.jinshu.api.dao.TenantMapper;
import com.jinshu.api.dao.UserMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Tenant;
import com.jinshu.common.entity.User;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.security.JwtTokenProvider;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthService 认证服务测试")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private TokenVersionManager tokenVersionManager;

    private AuthService authService;

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "Password1!";
    private static final String PASSWORD_HASH = "$2a$10$hashed_password";
    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, tenantMapper, passwordEncoder,
                jwtTokenProvider, tokenVersionManager);
        TenantContext.setTenantId(TENANT_ID);
        UserContext.setUserId(USER_ID);
        UserContext.setUsername(USERNAME);
        UserContext.setRole("ADMIN");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    private User createActiveUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setTenantId(TENANT_ID);
        user.setUsername(USERNAME);
        user.setPasswordHash(PASSWORD_HASH);
        user.setRole("ADMIN");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    @DisplayName("authenticate：正确用户名密码，返回 Token 和用户信息")
    void given_validCredentials_when_authenticate_then_returnTokens() {
        when(tokenVersionManager.getLoginFailCount(USERNAME)).thenReturn(0L);
        when(userMapper.selectByUsername(USERNAME)).thenReturn(createActiveUser());
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(tenantMapper.selectById(TENANT_ID)).thenReturn(createActiveTenant());
        when(jwtTokenProvider.createAccessToken(USER_ID, TENANT_ID, "ADMIN", 0)).thenReturn("access_token");
        when(jwtTokenProvider.createRefreshToken(USER_ID, TENANT_ID)).thenReturn("refresh_token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(1800000L);
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);

        Map<String, Object> result = authService.authenticate(USERNAME, PASSWORD);

        assertThat(result.get("token")).isEqualTo("access_token");
        assertThat(result.get("refreshToken")).isEqualTo("refresh_token");
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) result.get("user");
        assertThat(userInfo.get("id")).isEqualTo(USER_ID);
        assertThat(userInfo.get("tenantId")).isEqualTo(TENANT_ID);
        assertThat(userInfo.get("role")).isEqualTo("ADMIN");

        verify(tokenVersionManager).clearLoginFailCount(USERNAME);
    }

    private Tenant createActiveTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setStatus("ACTIVE");
        return tenant;
    }

    @Test
    @DisplayName("authenticate：账号已锁定抛异常")
    void given_lockedAccount_when_authenticate_then_throw() {
        when(tokenVersionManager.getLoginFailCount(USERNAME)).thenReturn(5L);
        when(tokenVersionManager.getLoginFailExpireTime(USERNAME)).thenReturn(600L);

        assertThatThrownBy(() -> authService.authenticate(USERNAME, PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号已锁定");

        verify(userMapper, never()).selectByUsername(any());
    }

    @Test
    @DisplayName("authenticate：用户不存在抛异常，失败计数 +1")
    void given_nonexistentUser_when_authenticate_then_throwAndIncrementFailCount() {
        when(tokenVersionManager.getLoginFailCount(USERNAME)).thenReturn(0L);
        when(userMapper.selectByUsername(USERNAME)).thenReturn(null);

        assertThatThrownBy(() -> authService.authenticate(USERNAME, PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.USERNAME_PASSWORD_ERROR.getCode());

        verify(tokenVersionManager).incrementLoginFailCount(USERNAME);
    }

    @Test
    @DisplayName("authenticate：用户已禁用抛异常")
    void given_disabledUser_when_authenticate_then_throw() {
        when(tokenVersionManager.getLoginFailCount(USERNAME)).thenReturn(0L);
        User disabledUser = createActiveUser();
        disabledUser.setStatus("DISABLED");
        when(userMapper.selectByUsername(USERNAME)).thenReturn(disabledUser);

        assertThatThrownBy(() -> authService.authenticate(USERNAME, PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.USER_DISABLED.getCode());
    }

    @Test
    @DisplayName("authenticate：密码错误抛异常，失败计数 +1")
    void given_wrongPassword_when_authenticate_then_throwAndIncrementFailCount() {
        when(tokenVersionManager.getLoginFailCount(USERNAME)).thenReturn(0L);
        when(userMapper.selectByUsername(USERNAME)).thenReturn(createActiveUser());
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(USERNAME, PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.USERNAME_PASSWORD_ERROR.getCode());

        verify(tokenVersionManager).incrementLoginFailCount(USERNAME);
    }

    @Test
    @DisplayName("authenticate：租户已禁用抛异常")
    void given_disabledTenant_when_authenticate_then_throw() {
        when(tokenVersionManager.getLoginFailCount(USERNAME)).thenReturn(0L);
        when(userMapper.selectByUsername(USERNAME)).thenReturn(createActiveUser());
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        Tenant disabledTenant = createActiveTenant();
        disabledTenant.setStatus("DISABLED");
        when(tenantMapper.selectById(TENANT_ID)).thenReturn(disabledTenant);

        assertThatThrownBy(() -> authService.authenticate(USERNAME, PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TENANT_DISABLED.getCode());
    }

    @Test
    @DisplayName("refreshToken：有效 Refresh Token 换发新 Access Token")
    void given_validRefreshToken_when_refresh_then_newAccessToken() {
        when(jwtTokenProvider.validateToken("valid_refresh_token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid_refresh_token")).thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken("valid_refresh_token")).thenReturn(TENANT_ID);
        when(jwtTokenProvider.getTypeFromToken("valid_refresh_token")).thenReturn("refresh");
        when(tokenVersionManager.isRefreshTokenValid(USER_ID, null)).thenReturn(true);
        when(userMapper.selectById(USER_ID)).thenReturn(createActiveUser());
        when(jwtTokenProvider.createAccessToken(USER_ID, TENANT_ID, "ADMIN", 0)).thenReturn("new_access_token");

        Map<String, Object> result = authService.refreshToken("valid_refresh_token");

        assertThat(result.get("token")).isEqualTo("new_access_token");
    }

    @Test
    @DisplayName("refreshToken：无效 Refresh Token 抛异常")
    void given_invalidRefreshToken_when_refresh_then_throw() {
        when(jwtTokenProvider.validateToken("bad_token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("bad_token"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.REFRESH_TOKEN_INVALID.getCode());
    }

    @Test
    @DisplayName("refreshToken：Token 类型非 refresh 抛异常")
    void given_accessTokenInsteadOfRefresh_when_refresh_then_throw() {
        when(jwtTokenProvider.validateToken("access_token")).thenReturn(true);
        when(jwtTokenProvider.getTypeFromToken("access_token")).thenReturn("access");

        assertThatThrownBy(() -> authService.refreshToken("access_token"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.REFRESH_TOKEN_INVALID.getCode());
    }

    @Test
    @DisplayName("getCurrentUser：已登录返回用户信息")
    void given_loggedIn_when_getCurrentUser_then_returnUserInfo() {
        when(userMapper.selectById(USER_ID)).thenReturn(createActiveUser());

        Map<String, Object> result = authService.getCurrentUser();

        assertThat(result.get("id")).isEqualTo(USER_ID);
        assertThat(result.get("username")).isEqualTo(USERNAME);
        assertThat(result.get("role")).isEqualTo("ADMIN");
        assertThat(result.get("tenantId")).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("getCurrentUser：未登录抛异常")
    void given_notLoggedIn_when_getCurrentUser_then_throw() {
        UserContext.clear();
        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.UNAUTHORIZED.getCode());
    }
}
