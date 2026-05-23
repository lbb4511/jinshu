package com.jinshu.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserContext 用户上下文测试")
class UserContextTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("设置用户 ID/用户名/角色后正确获取")
    void given_userInfo_when_setAndGet_then_match() {
        UserContext.setUserId(42L);
        UserContext.setUsername("zhangsan");
        UserContext.setRole("ADMIN");

        assertThat(UserContext.getUserId()).isEqualTo(42L);
        assertThat(UserContext.getUsername()).isEqualTo("zhangsan");
        assertThat(UserContext.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("清空后所有字段为 null")
    void given_userInfoSet_when_clear_then_allNull() {
        UserContext.setUserId(42L);
        UserContext.setUsername("zhangsan");
        UserContext.setRole("ADMIN");

        UserContext.clear();

        assertThat(UserContext.getUserId()).isNull();
        assertThat(UserContext.getUsername()).isNull();
        assertThat(UserContext.getRole()).isNull();
    }

    @Test
    @DisplayName("初始状态所有字段为 null")
    void given_noSet_when_get_then_allNull() {
        assertThat(UserContext.getUserId()).isNull();
        assertThat(UserContext.getUsername()).isNull();
        assertThat(UserContext.getRole()).isNull();
    }

    @Test
    @DisplayName("线程隔离：不同线程互不影响")
    void given_twoThreads_when_setDifferentUsers_then_isolated() throws InterruptedException {
        UserContext.setUserId(1L);

        Thread t2 = new Thread(() -> {
            UserContext.setUserId(99L);
            assertThat(UserContext.getUserId()).isEqualTo(99L);
            UserContext.clear();
            assertThat(UserContext.getUserId()).isNull();
        });

        t2.start();
        t2.join();

        assertThat(UserContext.getUserId()).isEqualTo(1L);
    }
}
