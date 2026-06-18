package com.jinshu.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段脱敏注解
 *
 * 标记实体类中需要脱敏的字段，配合 DesensitizeService 根据当前用户角色
 * 及 desensitize_rule 表中的规则动态脱敏。
 *
 * 使用示例：
 * <pre>
 * public class User {
 *     &#064;DesensitizeField(type = DesensitizeType.EMAIL, resourceType = "users", fieldName = "email")
 *     private String email;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DesensitizeField {

    /**
     * 脱敏规则类型
     */
    DesensitizeType type() default DesensitizeType.PHONE;

    /**
     * 资源类型（对应规则表的 table_name）。
     * 默认使用实体类简单名的 snake_case 复数形式。
     */
    String resourceType() default "";

    /**
     * 字段名（对应规则表的 column_name）。
     * 默认使用字段名的 snake_case。
     */
    String fieldName() default "";

    /**
     * 是否启用脱敏
     */
    boolean enabled() default true;
}
