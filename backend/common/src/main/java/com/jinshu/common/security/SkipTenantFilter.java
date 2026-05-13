package com.jinshu.common.security;

import java.lang.annotation.*;

/**
 * 跳过租户过滤注解
 * 用于标记不需要自动添加 tenant_id 过滤条件的 Mapper 方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkipTenantFilter {
}
