package com.jinshu.common.security;

import com.jinshu.common.context.UserContext;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * 角色与所有权检查切面
 *
 * 处理 &#64;RequireRole 与 &#64;RequireOwner 注解：
 * <ul>
 *   <li>&#64;RequireRole：校验当前用户角色是否在允许列表中；</li>
 *   <li>&#64;RequireOwner：ADMIN 直接放行，非 ADMIN 继续执行由 Service 层完成所有权校验。</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@Order(100)
public class RoleAspect {

    /**
     * 拦截 &#64;RequireRole 注解的方法或类
     *
     * @param joinPoint   连接点
     * @param requireRole 角色要求注解
     * @return 方法返回值
     * @throws Throwable 原方法异常或权限不足异常
     */
    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        String currentRole = UserContext.getRole();
        Set<String> allowedRoles = Set.of(requireRole.value());

        if (currentRole == null || !allowedRoles.contains(currentRole)) {
            log.warn("Access denied: required roles {}, actual role {}", allowedRoles, currentRole);
            throw new BusinessException(ErrorCode.ROLE_NOT_ALLOWED, "当前角色无权限执行此操作");
        }

        return joinPoint.proceed();
    }

    /**
     * 拦截 &#64;RequireOwner 注解的方法
     *
     * @param joinPoint    连接点
     * @param requireOwner 所有权要求注解
     * @return 方法返回值
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(requireOwner)")
    public Object checkOwner(ProceedingJoinPoint joinPoint, RequireOwner requireOwner) throws Throwable {
        String role = UserContext.getRole();
        if (Role.ADMIN.name().equals(role)) {
            return joinPoint.proceed();
        }

        // 非 ADMIN 角色继续执行，由 Service 层完成具体资源所有权校验
        log.debug("Non-admin access to owner-protected method, delegating ownership check to service layer");
        return joinPoint.proceed();
    }
}
