package com.jinshu.api.config;

import com.jinshu.api.ratelimit.RateLimitScope;
import com.jinshu.api.ratelimit.RateLimitService;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.exception.RateLimitException;
import com.jinshu.common.metrics.BusinessMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API 限流与租户并发配额拦截器
 *
 * 拦截所有 HTTP 请求，按以下顺序进行防护：
 * 1. 登录接口按客户端 IP 进行限流
 * 2. 已认证请求先获取租户并发配额
 * 3. 租户级滑动窗口限流
 * 4. 用户级滑动窗口限流
 *
 * 被限流时抛出 {@link RateLimitException}，由全局异常处理器返回 HTTP 429。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String CONCURRENCY_TENANT_ATTR = RateLimitInterceptor.class.getName() + ".tenantId";

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final BusinessMetrics businessMetrics;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled()) {
            return true;
        }

        String path = request.getRequestURI();

        // 登录接口按 IP 限流
        if (path.equals(properties.getLoginPath())) {
            String clientIp = getClientIp(request);
            if (!rateLimitService.checkRateLimit(RateLimitScope.LOGIN_IP, null, null, clientIp, path)) {
                businessMetrics.recordRateLimitHit(RateLimitScope.LOGIN_IP.name(), null);
                throw rateLimitException(RateLimitScope.LOGIN_IP);
            }
            return true;
        }

        Long tenantId = TenantContext.getTenantId();
        Long userId = UserContext.getUserId();

        // 租户并发配额 + 租户级限流
        if (tenantId != null) {
            if (!rateLimitService.acquireTenantConcurrency(tenantId)) {
                businessMetrics.recordRateLimitHit(RateLimitScope.TENANT.name(), tenantId);
                throw new RateLimitException(ErrorCode.TENANT_CONCURRENCY_EXCEEDED,
                        getTenantConcurrencyLimit(tenantId), 0, 1);
            }
            request.setAttribute(CONCURRENCY_TENANT_ATTR, tenantId);

            if (!rateLimitService.checkRateLimit(RateLimitScope.TENANT, tenantId, null, null, path)) {
                businessMetrics.recordRateLimitHit(RateLimitScope.TENANT.name(), tenantId);
                throw rateLimitException(RateLimitScope.TENANT);
            }
        }

        // 用户级限流
        if (userId != null) {
            if (!rateLimitService.checkRateLimit(RateLimitScope.USER, null, userId, null, path)) {
                businessMetrics.recordRateLimitHit(RateLimitScope.USER.name(), tenantId);
                throw rateLimitException(RateLimitScope.USER);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object tenantIdAttr = request.getAttribute(CONCURRENCY_TENANT_ATTR);
        if (tenantIdAttr instanceof Long tenantId) {
            rateLimitService.releaseTenantConcurrency(tenantId);
        }
    }

    private RateLimitException rateLimitException(RateLimitScope scope) {
        return switch (scope) {
            case USER -> new RateLimitException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    properties.getDefaultUserQps(), 1, 1);
            case TENANT -> new RateLimitException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    properties.getDefaultTenantQps(), 1, 1);
            case LOGIN_IP -> new RateLimitException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    properties.getDefaultLoginPerMinute(), 60, 60);
        };
    }

    private int getTenantConcurrencyLimit(Long tenantId) {
        // 这里只需要一个用于响应展示的阈值，默认返回配置默认值
        return properties.getDefaultTenantConcurrency();
    }

    /**
     * 获取客户端真实 IP，优先读取 X-Forwarded-For 头
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
