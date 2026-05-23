package com.jinshu.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TenantContext 多租户上下文测试")
class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("set/get：设置后能正确获取")
    void given_tenantId_when_setAndGet_then_match() {
        TenantContext.setTenantId(100L);
        assertThat(TenantContext.getTenantId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("清空后获取为 null")
    void given_tenantIdSet_when_clear_then_getReturnNull() {
        TenantContext.setTenantId(100L);
        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("初始状态为 null")
    void given_noSet_when_get_then_null() {
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("线程隔离：不同线程互不影响")
    void given_twoThreads_when_setDifferentTenants_then_isolated() throws InterruptedException {
        TenantContext.setTenantId(1L);

        Thread t2 = new Thread(() -> {
            TenantContext.setTenantId(99L);
            assertThat(TenantContext.getTenantId()).isEqualTo(99L);
            TenantContext.clear();
            assertThat(TenantContext.getTenantId()).isNull();
        });

        t2.start();
        t2.join();

        // 主线程不受影响
        assertThat(TenantContext.getTenantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("允许设置 null（用于绕过租户过滤）")
    void given_null_when_setAndGet_then_null() {
        TenantContext.setTenantId(null);
        assertThat(TenantContext.getTenantId()).isNull();
    }
}
