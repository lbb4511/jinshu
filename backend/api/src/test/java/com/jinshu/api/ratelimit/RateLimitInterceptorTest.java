package com.jinshu.api.ratelimit;

import com.jinshu.api.config.RateLimitInterceptor;
import com.jinshu.api.config.RateLimitProperties;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.exception.RateLimitException;
import com.jinshu.common.metrics.BusinessMetrics;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("RateLimitInterceptor 限流拦截器测试")
@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private BusinessMetrics businessMetrics;

    private RateLimitProperties properties;
    private RateLimitInterceptor interceptor;

    private final Long tenantId = 100L;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        interceptor = new RateLimitInterceptor(rateLimitService, properties, businessMetrics);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    @Test
    @DisplayName("限流关闭时直接放行")
    void given_disabled_when_preHandle_then_allow() {
        properties.setEnabled(false);
        HttpServletRequest request = mock(HttpServletRequest.class);

        boolean allowed = interceptor.preHandle(request, null, null);

        assertThat(allowed).isTrue();
        verifyNoInteractions(rateLimitService);
    }

    @Test
    @DisplayName("登录接口按 IP 限流")
    void given_loginPath_when_preHandle_then_checkLoginIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(rateLimitService.checkRateLimit(RateLimitScope.LOGIN_IP, null, null, "10.0.0.1", "/auth/login"))
                .thenReturn(true);

        boolean allowed = interceptor.preHandle(request, null, null);

        assertThat(allowed).isTrue();
        verify(rateLimitService).checkRateLimit(RateLimitScope.LOGIN_IP, null, null, "10.0.0.1", "/auth/login");
    }

    @Test
    @DisplayName("已认证请求通过租户并发、租户限流、用户限流检查")
    void given_authenticated_when_preHandle_then_allChecksPass() {
        TenantContext.setTenantId(tenantId);
        UserContext.setUserId(userId);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/reports");
        when(rateLimitService.acquireTenantConcurrency(tenantId)).thenReturn(true);
        when(rateLimitService.checkRateLimit(RateLimitScope.TENANT, tenantId, null, null, "/reports"))
                .thenReturn(true);
        when(rateLimitService.checkRateLimit(RateLimitScope.USER, null, userId, null, "/reports"))
                .thenReturn(true);

        boolean allowed = interceptor.preHandle(request, null, null);

        assertThat(allowed).isTrue();
        verify(rateLimitService).acquireTenantConcurrency(tenantId);
        verify(rateLimitService).checkRateLimit(RateLimitScope.TENANT, tenantId, null, null, "/reports");
        verify(rateLimitService).checkRateLimit(RateLimitScope.USER, null, userId, null, "/reports");
    }

    @Test
    @DisplayName("租户并发超过配额时抛出限流异常")
    void given_tenantConcurrencyExceeded_when_preHandle_then_throw() {
        TenantContext.setTenantId(tenantId);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/reports");
        when(rateLimitService.acquireTenantConcurrency(tenantId)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOf(RateLimitException.class)
                .satisfies(ex -> {
                    RateLimitException re = (RateLimitException) ex;
                    assertThat(re.getCode()).isEqualTo(ErrorCode.TENANT_CONCURRENCY_EXCEEDED.getCode());
                });
    }

    @Test
    @DisplayName("租户级限流超过阈值时抛出限流异常")
    void given_tenantRateLimitExceeded_when_preHandle_then_throw() {
        TenantContext.setTenantId(tenantId);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/reports");
        when(rateLimitService.acquireTenantConcurrency(tenantId)).thenReturn(true);
        when(rateLimitService.checkRateLimit(RateLimitScope.TENANT, tenantId, null, null, "/reports"))
                .thenReturn(false);

        assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOf(RateLimitException.class)
                .satisfies(ex -> assertThat(((RateLimitException) ex).getCode())
                        .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED.getCode()));
    }

    @Test
    @DisplayName("afterCompletion 释放已获取的租户并发配额")
    void given_tenantAcquired_when_afterCompletion_then_release() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(RateLimitInterceptor.class.getName() + ".tenantId")).thenReturn(tenantId);

        interceptor.afterCompletion(request, null, null, null);

        verify(rateLimitService).releaseTenantConcurrency(tenantId);
    }
}
