package com.jinshu.api.integration;

import com.jinshu.api.dao.DesensitizeRuleMapper;
import com.jinshu.api.service.DesensitizeService;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.DesensitizeRule;
import com.jinshu.common.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DesensitizeService - DB integration tests")
class DesensitizeServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DesensitizeService desensitizeService;

    @Autowired
    private DesensitizeRuleMapper ruleMapper;

    @AfterEach
    void resetContext() {
        UserContext.setRole(ROLE);
    }

    @Test
    @DisplayName("USER 角色按全局默认规则脱敏")
    @Transactional
    void shouldDesensitizeForUserRole() {
        UserContext.setRole("USER");

        User user = new User();
        user.setTenantId(TENANT_ID);
        user.setUsername("zhangsan");
        user.setDisplayName("张三丰");
        user.setEmail("zhangsan@example.com");

        desensitizeService.desensitize(user);

        assertThat(user.getEmail()).isEqualTo("z***@example.com");
        assertThat(user.getDisplayName()).isEqualTo("张**");
    }

    @Test
    @DisplayName("ADMIN 角色不脱敏")
    @Transactional
    void shouldNotDesensitizeForAdminRole() {
        UserContext.setRole("ADMIN");

        User user = new User();
        user.setTenantId(TENANT_ID);
        user.setDisplayName("张三丰");
        user.setEmail("zhangsan@example.com");

        desensitizeService.desensitize(user);

        assertThat(user.getEmail()).isEqualTo("zhangsan@example.com");
        assertThat(user.getDisplayName()).isEqualTo("张三丰");
    }

    @Test
    @DisplayName("租户级规则可覆盖全局规则")
    @Transactional
    void tenantRuleCanOverrideGlobalRule() {
        UserContext.setRole("USER");

        DesensitizeRule tenantRule = new DesensitizeRule();
        tenantRule.setTenantId(TENANT_ID);
        tenantRule.setTableName("users");
        tenantRule.setColumnName("email");
        tenantRule.setRuleType("EMAIL");
        tenantRule.setApplicableRoles("VIEWER");
        tenantRule.setEnabled(true);
        tenantRule.setCreatedAt(LocalDateTime.now());
        tenantRule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.insert(tenantRule);

        User user = new User();
        user.setTenantId(TENANT_ID);
        user.setEmail("zhangsan@example.com");

        desensitizeService.desensitize(user);

        assertThat(user.getEmail()).isEqualTo("zhangsan@example.com");
    }

    @Test
    @DisplayName("listRules 返回租户规则")
    @Transactional
    void shouldListTenantRules() {
        DesensitizeRule rule = new DesensitizeRule();
        rule.setTenantId(TENANT_ID);
        rule.setTableName("report_data");
        rule.setColumnName("salary");
        rule.setRuleType("AMOUNT");
        rule.setApplicableRoles("VIEWER");
        rule.setEnabled(true);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.insert(rule);

        var rules = desensitizeService.listRules(TENANT_ID);

        assertThat(rules).anyMatch(r ->
                "report_data".equals(r.getTableName()) && "salary".equals(r.getColumnName()));
    }
}
