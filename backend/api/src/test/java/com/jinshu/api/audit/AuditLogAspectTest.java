package com.jinshu.api.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.audit.AuditLogEvent;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuditLogAspect 审计切面测试")
class AuditLogAspectTest {

    private final AuditLogAspect aspect = new AuditLogAspect(new ObjectMapper());
    private final DemoService proxy;

    public AuditLogAspectTest() {
        DemoService target = new DemoService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        this.proxy = factory.getProxy();
    }

    @BeforeEach
    void setUpContext() {
        TenantContext.setTenantId(1L);
        UserContext.setUserId(2L);
        UserContext.setUsername("tester");
        AuditLogger.getQueue().clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
        AuditLogger.getQueue().clear();
    }

    @Test
    @DisplayName("正常调用应生成 SUCCESS 审计事件")
    void given_successfulCall_when_around_then_successEvent() {
        String result = proxy.createItem("report-1");

        assertThat(result).isEqualTo("created:report-1");
        AuditLogEvent event = AuditLogger.getQueue().poll();
        assertThat(event).isNotNull();
        assertThat(event.getOperation()).isEqualTo("CREATE_ITEM");
        assertThat(event.getTargetType()).isEqualTo("ITEM");
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
        assertThat(event.getTenantId()).isEqualTo(1L);
        assertThat(event.getUserId()).isEqualTo(2L);
        assertThat(event.getUsername()).isEqualTo("tester");
        assertThat(event.getDuration()).isGreaterThanOrEqualTo(0);
        assertThat(event.getRequestParams()).contains("report-1");
    }

    @Test
    @DisplayName("异常调用应生成 FAILED 审计事件并继续抛异常")
    void given_failingCall_when_around_then_failedEventAndRethrow() {
        assertThatThrownBy(() -> proxy.deleteItem(42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("delete failed");

        AuditLogEvent event = AuditLogger.getQueue().poll();
        assertThat(event).isNotNull();
        assertThat(event.getOperation()).isEqualTo("DELETE_ITEM");
        assertThat(event.getStatus()).isEqualTo("FAILED");
        assertThat(event.getErrorMessage()).isEqualTo("delete failed");
        assertThat(event.getTargetId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("敏感参数应被过滤")
    void given_sensitiveParams_when_around_then_filtered() {
        proxy.login("admin", "secret123", "token-abc");

        AuditLogEvent event = AuditLogger.getQueue().poll();
        assertThat(event).isNotNull();
        assertThat(event.getRequestParams()).doesNotContain("secret123", "token-abc");
        assertThat(event.getRequestParams()).contains("admin");
    }

    static class DemoService {

        @AuditLog(operation = "CREATE_ITEM", targetType = "ITEM")
        public String createItem(String name) {
            return "created:" + name;
        }

        @AuditLog(operation = "DELETE_ITEM", targetType = "ITEM")
        public String deleteItem(Long id) {
            throw new RuntimeException("delete failed");
        }

        @AuditLog(operation = "LOGIN", targetType = "USER")
        public String login(String username, String password, String token) {
            return "ok";
        }
    }
}
