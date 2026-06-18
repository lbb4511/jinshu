package com.jinshu.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 资源所有权检查标记注解
 *
 * 标注在需要校验资源所有权的方法上。AOP 层仅做角色过滤：
 * ADMIN 角色直接放行；非 ADMIN 角色继续执行，由 Service 层完成具体所有权校验。
 *
 * 使用示例：
 * <pre>
 *   &#64;RequireRole({"ADMIN", "USER"})
 *   &#64;RequireOwner(resourceIdParam = "id")
 *   public Result&lt;Report&gt; updateReport(&#64;PathVariable Long id, ...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireOwner {

    /**
     * 方法参数中资源 ID 的参数名
     *
     * @return 参数名，默认为 "id"
     */
    String resourceIdParam() default "id";
}
