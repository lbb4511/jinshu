package com.jinshu.common.security;

import com.jinshu.common.common.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object auditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String userId = String.valueOf(UserContext.getUserId());
        String username = UserContext.getUsername();
        String operation = joinPoint.getSignature().getName();
        String ip = request != null ? request.getRemoteAddr() : "";

        log.info("审计日志 - 用户: {}({}), 操作: {}, IP: {}", username, userId, operation, ip);

        Object result;
        try {
            result = joinPoint.proceed();
            log.info("审计日志 - 操作成功: {}", operation);
        } catch (Throwable e) {
            log.error("审计日志 - 操作失败: {}, 错误: {}", operation, e.getMessage());
            throw e;
        }

        return result;
    }
}
