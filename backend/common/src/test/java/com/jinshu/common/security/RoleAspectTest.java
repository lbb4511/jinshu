package com.jinshu.common.security;

import com.jinshu.common.context.UserContext;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoleAspect 角色切面测试")
class RoleAspectTest {

    private final RoleAspect aspect = new RoleAspect();
    private final DemoService proxy;

    public RoleAspectTest() {
        DemoService target = new DemoService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        this.proxy = factory.getProxy();
    }

    @BeforeEach
    void setUp() {
        UserContext.setUserId(1L);
        UserContext.setRole("ADMIN");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("满足角色要求时正常执行")
    void given_allowedRole_when_call_then_success() {
        String result = proxy.adminOnly();
        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("不满足角色要求时抛出 403")
    void given_disallowedRole_when_call_then_throw() {
        UserContext.setRole("VIEWER");
        assertThatThrownBy(() -> proxy.adminOnly())
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.ROLE_NOT_ALLOWED.getCode()));
    }

    @Test
    @DisplayName("多角色要求时任一匹配即可执行")
    void given_anyMatchedRole_when_call_then_success() {
        UserContext.setRole("USER");
        assertThat(proxy.adminOrUser()).isEqualTo("ok");
    }

    @Test
    @DisplayName("未登录时访问受保护方法抛出 403")
    void given_noRole_when_call_then_throw() {
        UserContext.clear();
        assertThatThrownBy(() -> proxy.adminOnly())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("RequireOwner 对 ADMIN 直接放行")
    void given_admin_when_ownerRequired_then_success() {
        assertThat(proxy.ownerRequired(1L)).isEqualTo("ok:1");
    }

    @Test
    @DisplayName("RequireOwner 对非 ADMIN 继续执行")
    void given_nonAdmin_when_ownerRequired_then_proceed() {
        UserContext.setRole("USER");
        assertThat(proxy.ownerRequired(1L)).isEqualTo("ok:1");
    }

    static class DemoService {

        @RequireRole("ADMIN")
        public String adminOnly() {
            return "ok";
        }

        @RequireRole({"ADMIN", "USER"})
        public String adminOrUser() {
            return "ok";
        }

        @RequireOwner(resourceIdParam = "id")
        public String ownerRequired(Long id) {
            return "ok:" + id;
        }
    }
}
