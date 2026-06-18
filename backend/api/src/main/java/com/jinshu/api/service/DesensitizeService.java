package com.jinshu.api.service;

import com.jinshu.api.dao.DesensitizeRuleMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.DesensitizeRule;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.security.DesensitizeField;
import com.jinshu.common.security.DesensitizeType;
import com.jinshu.common.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 敏感数据脱敏服务
 *
 * 根据当前用户角色及 sys.desensitize_rule 表规则，对标注了
 * &#064;DesensitizeField 的字段进行动态脱敏。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesensitizeService {

    private final DesensitizeRuleMapper ruleMapper;

    private static final Set<String> EXEMPT_ROLES = Set.of(
            Role.ADMIN.name(), Role.SECURITY_ADMIN.name(), Role.AUDITOR.name()
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})\\d{8}(\\d{4})");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{8,12}(\\d{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.).*@(.*)$");

    /**
     * 按指定脱敏类型对单个值脱敏。
     *
     * @param value 原始值
     * @param type  脱敏类型
     * @return 脱敏后的值；管理员/安全管理员/审计员角色返回原值
     */
    public String desensitize(String value, DesensitizeType type) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String role = UserContext.getRole();
        if (isExempt(role)) {
            return value;
        }
        return switch (type) {
            case PHONE -> maskPhone(value);
            case ID_CARD -> maskIdCard(value);
            case BANK_CARD -> maskBankCard(value);
            case EMAIL -> maskEmail(value);
            case NAME -> maskName(value);
            case AMOUNT -> maskAmount(value, role);
            case FULL_MASK -> "****";
        };
    }

    /**
     * 对对象（或集合、分页结果）中标注了 &#064;DesensitizeField 的 String 字段进行脱敏。
     *
     * @param target 目标对象
     */
    public void desensitize(Object target) {
        if (target == null) {
            return;
        }
        if (target instanceof Collection<?> collection) {
            collection.forEach(this::desensitize);
            return;
        }
        if (target instanceof PageResult<?> pageResult) {
            if (pageResult.getList() != null) {
                pageResult.getList().forEach(this::desensitize);
            }
            return;
        }
        desensitizeObject(target);
    }

    /**
     * 查询指定租户的全部脱敏规则（不含全局规则）。
     *
     * @param tenantId 租户ID
     * @return 规则列表
     */
    public List<DesensitizeRule> listRules(Long tenantId) {
        return ruleMapper.selectByTenantId(tenantId);
    }

    private void desensitizeObject(Object target) {
        String currentRole = UserContext.getRole();
        if (isExempt(currentRole)) {
            return;
        }
        Long tenantId = TenantContext.getTenantId();
        Class<?> clazz = target.getClass();
        for (Field field : getAllFields(clazz)) {
            DesensitizeField annotation = field.getAnnotation(DesensitizeField.class);
            if (annotation == null || !annotation.enabled()) {
                continue;
            }
            if (!String.class.equals(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                String original = (String) field.get(target);
                if (original == null) {
                    continue;
                }
                String tableName = StringUtils.hasText(annotation.resourceType())
                        ? annotation.resourceType()
                        : defaultResourceType(clazz);
                String columnName = StringUtils.hasText(annotation.fieldName())
                        ? annotation.fieldName()
                        : camelToSnake(field.getName());

                DesensitizeRule rule = findRule(tenantId, tableName, columnName);
                if (rule != null) {
                    if (Boolean.FALSE.equals(rule.getEnabled())) {
                        continue;
                    }
                    if (!isRoleApplicable(currentRole, rule.getApplicableRoles())) {
                        continue;
                    }
                }
                String masked = desensitize(original, annotation.type());
                field.set(target, masked);
            } catch (IllegalAccessException e) {
                log.warn("Failed to desensitize field {} of {}", field.getName(), clazz.getName(), e);
            }
        }
    }

    private DesensitizeRule findRule(Long tenantId, String tableName, String columnName) {
        DesensitizeRule rule = null;
        if (tenantId != null) {
            rule = ruleMapper.selectByTenantAndField(tenantId, tableName, columnName);
        }
        if (rule == null) {
            rule = ruleMapper.selectByTenantAndField(0L, tableName, columnName);
        }
        return rule;
    }

    private boolean isRoleApplicable(String role, String applicableRoles) {
        if (!StringUtils.hasText(applicableRoles)) {
            return true;
        }
        Set<String> roles = Arrays.stream(applicableRoles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        return roles.isEmpty() || roles.contains(role);
    }

    private static boolean isExempt(String role) {
        return role != null && EXEMPT_ROLES.contains(role);
    }

    private static String defaultResourceType(Class<?> clazz) {
        String snake = camelToSnake(clazz.getSimpleName());
        return snake + "s";
    }

    private static String camelToSnake(String str) {
        if (!StringUtils.hasText(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (!sb.isEmpty()) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String maskPhone(String value) {
        return PHONE_PATTERN.matcher(value).replaceAll("$1****$2");
    }

    private static String maskIdCard(String value) {
        return ID_CARD_PATTERN.matcher(value).replaceAll("$1********$2");
    }

    private static String maskBankCard(String value) {
        return BANK_CARD_PATTERN.matcher(value).replaceAll("$1******$2");
    }

    private static String maskEmail(String value) {
        return EMAIL_PATTERN.matcher(value).replaceAll("$1***@$2");
    }

    private static String maskName(String value) {
        if (value.length() <= 2) {
            return value.charAt(0) + "*";
        }
        return value.charAt(0) + "*".repeat(value.length() - 1);
    }

    private static String maskAmount(String value, String role) {
        if (Role.VIEWER.name().equals(role)) {
            return "***";
        }
        return value;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}
