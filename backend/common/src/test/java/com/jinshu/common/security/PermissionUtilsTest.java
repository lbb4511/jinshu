package com.jinshu.common.security;

import com.jinshu.common.context.UserContext;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PermissionUtils 权限检查工具测试")
class PermissionUtilsTest {

    @BeforeEach
    void setUp() {
        UserContext.setUserId(1L);
        UserContext.setRole("USER");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("checkRole：允许列表包含当前角色时不抛异常")
    void given_allowedRole_when_checkRole_then_noException() {
        PermissionUtils.checkRole("ADMIN", "USER");
    }

    @Test
    @DisplayName("checkRole：当前角色不在允许列表时抛 20001")
    void given_disallowedRole_when_checkRole_then_throw() {
        UserContext.setRole("VIEWER");
        assertThatThrownBy(() -> PermissionUtils.checkRole("ADMIN", "USER"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ROLE_NOT_ALLOWED.getCode());
    }

    @Test
    @DisplayName("checkRole(Role...)：使用枚举同样生效")
    void given_roleEnum_when_checkRole_then_work() {
        PermissionUtils.checkRole(Role.ADMIN, Role.USER);
    }

    @Test
    @DisplayName("checkOwner：资源所有者本人不抛异常")
    void given_owner_when_checkOwner_then_noException() {
        PermissionUtils.checkOwner(1L);
    }

    @Test
    @DisplayName("checkOwner：ADMIN 访问他人资源不抛异常")
    void given_admin_when_checkOwner_then_noException() {
        UserContext.setRole("ADMIN");
        PermissionUtils.checkOwner(999L);
    }

    @Test
    @DisplayName("checkOwner：非所有者非 ADMIN 抛 20002")
    void given_nonOwner_when_checkOwner_then_throw() {
        assertThatThrownBy(() -> PermissionUtils.checkOwner(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_RESOURCE_OWNER.getCode());
    }

    @Test
    @DisplayName("checkReviewer：指派的审查人本人不抛异常")
    void given_reviewer_when_checkReviewer_then_noException() {
        PermissionUtils.checkReviewer(1L);
    }

    @Test
    @DisplayName("checkReviewer：ADMIN 可审批他人指派的报表")
    void given_admin_when_checkReviewer_then_noException() {
        UserContext.setRole("ADMIN");
        PermissionUtils.checkReviewer(999L);
    }

    @Test
    @DisplayName("checkReviewer：非指派审查人非 ADMIN 抛 20003")
    void given_nonReviewer_when_checkReviewer_then_throw() {
        assertThatThrownBy(() -> PermissionUtils.checkReviewer(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_ASSIGNED_REVIEWER.getCode());
    }
}
