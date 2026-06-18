package com.jinshu.api.service;

import com.jinshu.api.dao.DesensitizeRuleMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.DesensitizeRule;
import com.jinshu.common.entity.User;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.security.DesensitizeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DesensitizeService 敏感数据脱敏服务测试")
@ExtendWith(MockitoExtension.class)
class DesensitizeServiceTest {

    @Mock
    private DesensitizeRuleMapper ruleMapper;

    private DesensitizeService desensitizeService;

    @BeforeEach
    void setUp() {
        desensitizeService = new DesensitizeService(ruleMapper);
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    @Test
    @DisplayName("PHONE 规则：USER 角色脱敏，ADMIN 角色明文")
    void given_phone_when_desensitize_then_roleBased() {
        UserContext.setRole("USER");
        assertThat(desensitizeService.desensitize("13812345678", DesensitizeType.PHONE))
                .isEqualTo("138****5678");

        UserContext.setRole("ADMIN");
        assertThat(desensitizeService.desensitize("13812345678", DesensitizeType.PHONE))
                .isEqualTo("13812345678");
    }

    @Test
    @DisplayName("ID_CARD 规则：中间 8 位替换为 *")
    void given_idCard_when_desensitize_then_masked() {
        UserContext.setRole("USER");
        assertThat(desensitizeService.desensitize("110101199001011234", DesensitizeType.ID_CARD))
                .isEqualTo("110101********1234");
    }

    @Test
    @DisplayName("BANK_CARD 规则：保留前后 4 位")
    void given_bankCard_when_desensitize_then_masked() {
        UserContext.setRole("USER");
        assertThat(desensitizeService.desensitize("6222021234567890123", DesensitizeType.BANK_CARD))
                .isEqualTo("6222******0123");
    }

    @Test
    @DisplayName("EMAIL 规则：保留首字符和域名")
    void given_email_when_desensitize_then_masked() {
        UserContext.setRole("USER");
        assertThat(desensitizeService.desensitize("zhangsan@example.com", DesensitizeType.EMAIL))
                .isEqualTo("z***@example.com");
    }

    @Test
    @DisplayName("NAME 规则：保留首字")
    void given_name_when_desensitize_then_masked() {
        UserContext.setRole("USER");
        assertThat(desensitizeService.desensitize("张三丰", DesensitizeType.NAME))
                .isEqualTo("张**");
        assertThat(desensitizeService.desensitize("张三", DesensitizeType.NAME))
                .isEqualTo("张*");
    }

    @Test
    @DisplayName("AMOUNT 规则：VIEWER 全掩码，USER 明文")
    void given_amount_when_desensitize_then_roleBased() {
        UserContext.setRole("VIEWER");
        assertThat(desensitizeService.desensitize("125680.50", DesensitizeType.AMOUNT))
                .isEqualTo("***");

        UserContext.setRole("USER");
        assertThat(desensitizeService.desensitize("125680.50", DesensitizeType.AMOUNT))
                .isEqualTo("125680.50");
    }

    @Test
    @DisplayName("FULL_MASK 规则：任意值显示为 ****")
    void given_fullMask_when_desensitize_then_masked() {
        UserContext.setRole("USER");
        assertThat(desensitizeService.desensitize("any value", DesensitizeType.FULL_MASK))
                .isEqualTo("****");
    }

    @Test
    @DisplayName("空值或 null 不脱敏")
    void given_emptyValue_when_desensitize_then_unchanged() {
        UserContext.setRole("USER");
        assertThat(desensitizeService.desensitize("", DesensitizeType.PHONE)).isEmpty();
        assertThat(desensitizeService.desensitize(null, DesensitizeType.PHONE)).isNull();
    }

    @Test
    @DisplayName("对象脱敏：默认对 USER 角色生效")
    void given_userObject_when_desensitize_then_fieldsMasked() {
        UserContext.setRole("USER");
        when(ruleMapper.selectByTenantAndField(any(), any(), any())).thenReturn(null);

        User user = createUser();
        desensitizeService.desensitize(user);

        assertThat(user.getEmail()).isEqualTo("z***@example.com");
        assertThat(user.getDisplayName()).isEqualTo("张*");
        assertThat(user.getUsername()).isEqualTo("zhangsan");
    }

    @Test
    @DisplayName("对象脱敏：ADMIN 角色直接跳过")
    void given_adminRole_when_desensitizeObject_then_unchanged() {
        UserContext.setRole("ADMIN");

        User user = createUser();
        desensitizeService.desensitize(user);

        assertThat(user.getEmail()).isEqualTo("zhangsan@example.com");
        assertThat(user.getDisplayName()).isEqualTo("张三");
        verifyNoInteractions(ruleMapper);
    }

    @Test
    @DisplayName("对象脱敏：规则表未命中当前角色时不脱敏")
    void given_ruleNotApplicable_when_desensitizeObject_then_unchanged() {
        UserContext.setRole("USER");
        DesensitizeRule rule = new DesensitizeRule();
        rule.setTenantId(1L);
        rule.setTableName("users");
        rule.setColumnName("email");
        rule.setRuleType("EMAIL");
        rule.setApplicableRoles("VIEWER");
        rule.setEnabled(true);
        when(ruleMapper.selectByTenantAndField(1L, "users", "email")).thenReturn(rule);
        when(ruleMapper.selectByTenantAndField(1L, "users", "display_name")).thenReturn(null);

        User user = createUser();
        desensitizeService.desensitize(user);

        assertThat(user.getEmail()).isEqualTo("zhangsan@example.com");
        assertThat(user.getDisplayName()).isEqualTo("张*");
    }

    @Test
    @DisplayName("对象脱敏：禁用规则时不脱敏")
    void given_disabledRule_when_desensitizeObject_then_unchanged() {
        UserContext.setRole("USER");
        DesensitizeRule rule = new DesensitizeRule();
        rule.setEnabled(false);
        when(ruleMapper.selectByTenantAndField(1L, "users", "email")).thenReturn(rule);
        when(ruleMapper.selectByTenantAndField(1L, "users", "display_name")).thenReturn(null);

        User user = createUser();
        desensitizeService.desensitize(user);

        assertThat(user.getEmail()).isEqualTo("zhangsan@example.com");
    }

    @Test
    @DisplayName("对象脱敏：未命中租户规则时回退到全局规则")
    void given_noTenantRule_when_desensitizeObject_then_fallbackToGlobal() {
        UserContext.setRole("USER");
        DesensitizeRule globalRule = new DesensitizeRule();
        globalRule.setTenantId(0L);
        globalRule.setTableName("users");
        globalRule.setColumnName("email");
        globalRule.setRuleType("EMAIL");
        globalRule.setApplicableRoles("USER,VIEWER");
        globalRule.setEnabled(true);
        when(ruleMapper.selectByTenantAndField(1L, "users", "email")).thenReturn(null);
        when(ruleMapper.selectByTenantAndField(0L, "users", "email")).thenReturn(globalRule);
        when(ruleMapper.selectByTenantAndField(1L, "users", "display_name")).thenReturn(null);
        when(ruleMapper.selectByTenantAndField(0L, "users", "display_name")).thenReturn(null);

        User user = createUser();
        desensitizeService.desensitize(user);

        assertThat(user.getEmail()).isEqualTo("z***@example.com");
    }

    @Test
    @DisplayName("集合与分页结果脱敏")
    void given_collectionAndPage_when_desensitize_then_allMasked() {
        UserContext.setRole("USER");
        when(ruleMapper.selectByTenantAndField(any(), any(), any())).thenReturn(null);

        User user1 = createUser();
        User user2 = createUser();
        user2.setEmail("lisi@example.com");
        user2.setDisplayName("李四");

        List<User> list = List.of(user1, user2);
        desensitizeService.desensitize(list);
        assertThat(user1.getEmail()).isEqualTo("z***@example.com");
        assertThat(user2.getEmail()).isEqualTo("l***@example.com");

        User user3 = createUser();
        PageResult<User> page = PageResult.of(List.of(user3), 1, 1, 10);
        desensitizeService.desensitize(page);
        assertThat(user3.getEmail()).isEqualTo("z***@example.com");
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setTenantId(1L);
        user.setUsername("zhangsan");
        user.setDisplayName("张三");
        user.setEmail("zhangsan@example.com");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
