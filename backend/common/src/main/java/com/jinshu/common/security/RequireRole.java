package com.jinshu.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级角色要求注解
 *
 * 标注在 Controller 方法或类上，只允许指定角色的用户访问。
 * 支持传入一个或多个角色标识，如 {"ADMIN", "USER"}。
 *
 * 使用示例：
 * <pre>
 *   &#64;RequireRole({"ADMIN", "USER"})
 *   public Result&lt;Report&gt; createReport(...) { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色列表
     *
     * @return 角色名称数组
     */
    String[] value();
}
