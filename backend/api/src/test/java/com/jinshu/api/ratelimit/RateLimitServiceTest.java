package com.jinshu.api.ratelimit;

import com.jinshu.api.config.RateLimitProperties;
import com.jinshu.api.dao.RateLimitConfigMapper;
import com.jinshu.api.dao.TenantConcurrencyQuotaMapper;
import com.jinshu.common.entity.RateLimitConfig;
import com.jinshu.common.entity.TenantConcurrencyQuota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("RateLimitService 限流服务测试")
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RateLimitConfigMapper rateLimitConfigMapper;

    @Mock
    private TenantConcurrencyQuotaMapper concurrencyQuotaMapper;

    private RateLimitProperties properties;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        rateLimitService = new RateLimitService(redisTemplate, rateLimitConfigMapper, concurrencyQuotaMapper, properties);
    }

    @Test
    @DisplayName("用户级限流：未超过阈值时允许通过")
    void given_underUserLimit_when_check_then_allow() {
        when(rateLimitConfigMapper.selectGlobalByScope("USER")).thenReturn(null);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(50L);

        boolean allowed = rateLimitService.checkRateLimit(RateLimitScope.USER, null, 1L, null, "/reports");

        assertThat(allowed).isTrue();
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    @DisplayName("用户级限流：超过阈值时拒绝")
    void given_overUserLimit_when_check_then_deny() {
        RateLimitConfig config = new RateLimitConfig();
        config.setScope("USER");
        config.setMaxRequests(10);
        config.setWindowSeconds(1);
        config.setEnabled(true);
        when(rateLimitConfigMapper.selectGlobalByScope("USER")).thenReturn(config);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(11L);

        boolean allowed = rateLimitService.checkRateLimit(RateLimitScope.USER, null, 1L, null, "/reports");

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("租户级限流：优先使用租户级配置")
    void given_tenantSpecificConfig_when_check_then_useTenantConfig() {
        RateLimitConfig tenantConfig = new RateLimitConfig();
        tenantConfig.setScope("TENANT");
        tenantConfig.setTenantId(100L);
        tenantConfig.setMaxRequests(50);
        tenantConfig.setWindowSeconds(1);
        tenantConfig.setEnabled(true);
        when(rateLimitConfigMapper.selectByScopeAndTenant("TENANT", 100L)).thenReturn(tenantConfig);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        boolean allowed = rateLimitService.checkRateLimit(RateLimitScope.TENANT, 100L, null, null, "/reports");

        assertThat(allowed).isTrue();
        verify(rateLimitConfigMapper).selectByScopeAndTenant("TENANT", 100L);
        verify(rateLimitConfigMapper, never()).selectGlobalByScope("TENANT");
    }

    @Test
    @DisplayName("登录 IP 限流：使用全局配置")
    void given_loginRequest_when_check_then_useLoginIpConfig() {
        RateLimitConfig config = new RateLimitConfig();
        config.setScope("LOGIN_IP");
        config.setMaxRequests(5);
        config.setWindowSeconds(60);
        config.setEnabled(true);
        when(rateLimitConfigMapper.selectGlobalByScope("LOGIN_IP")).thenReturn(config);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(3L);

        boolean allowed = rateLimitService.checkRateLimit(RateLimitScope.LOGIN_IP, null, null, "192.168.1.1", "/auth/login");

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("租户并发配额：未超过阈值时获取成功")
    void given_underConcurrencyLimit_when_acquire_then_success() {
        TenantConcurrencyQuota quota = new TenantConcurrencyQuota();
        quota.setTenantId(100L);
        quota.setMaxConcurrent(10);
        quota.setEnabled(true);
        when(concurrencyQuotaMapper.selectByTenantId(100L)).thenReturn(quota);
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("jinshu:ratelimit:concurrent:tenant:100")), any(Object[].class)))
                .thenReturn(5L);

        boolean acquired = rateLimitService.acquireTenantConcurrency(100L);

        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("租户并发配额：超过阈值时获取失败")
    void given_overConcurrencyLimit_when_acquire_then_fail() {
        TenantConcurrencyQuota quota = new TenantConcurrencyQuota();
        quota.setTenantId(100L);
        quota.setMaxConcurrent(5);
        quota.setEnabled(true);
        when(concurrencyQuotaMapper.selectByTenantId(100L)).thenReturn(quota);
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("jinshu:ratelimit:concurrent:tenant:100")), any(Object[].class)))
                .thenReturn(-1L);

        boolean acquired = rateLimitService.acquireTenantConcurrency(100L);

        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("租户并发配额：释放时调用 Redis DECR 脚本")
    void given_acquired_when_release_then_executeDecrScript() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("jinshu:ratelimit:concurrent:tenant:100")), any(Object[].class)))
                .thenReturn(4L);

        rateLimitService.releaseTenantConcurrency(100L);

        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("jinshu:ratelimit:concurrent:tenant:100")), any(Object[].class));
    }
}
