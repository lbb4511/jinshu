package com.jinshu.api.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "jinshu.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogAspect {

    private final ObjectMapper objectMapper;

    @Around("@annotation(com.jinshu.common.audit.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        com.jinshu.common.audit.AuditLog auditLog = method.getAnnotation(com.jinshu.common.audit.AuditLog.class);

        String operation = auditLog.operation();
        String targetType = auditLog.targetType();
        String targetName = auditLog.targetName();

        Long targetId = extractTargetId(joinPoint.getArgs());
        Map<String, Object> requestParams = extractRequestParams(joinPoint.getArgs(), signature.getParameterNames());

        HttpServletRequest request = getCurrentRequest();
        String ipAddress = request != null ? request.getRemoteAddr() : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        Object result = null;
        String status = "SUCCESS";
        String errorMessage = null;
        long startTime = System.currentTimeMillis();
        long duration = 0;

        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            throw e;
        } finally {
            duration = System.currentTimeMillis() - startTime;
            try {
                com.jinshu.common.audit.AuditLogEvent event = com.jinshu.common.audit.AuditLogEvent.builder()
                        .tenantId(TenantContext.getTenantId())
                        .userId(UserContext.getUserId())
                        .username(UserContext.getUsername())
                        .operation(operation)
                        .targetType(targetType)
                        .targetId(targetId)
                        .targetName(targetName)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .requestParams(serializeParams(requestParams))
                        .status(status)
                        .errorMessage(errorMessage)
                        .duration((int) duration)
                        .createdAt(LocalDateTime.now())
                        .build();

                AuditLogger.getQueue().offer(event);
            } catch (Exception e) {
                log.error("Failed to log audit event: {}", e.getMessage());
            }
        }

        return result;
    }

    private Long extractTargetId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }

    private Map<String, Object> extractRequestParams(Object[] args, String[] paramNames) {
        Map<String, Object> params = new HashMap<>();
        if (args == null || paramNames == null) {
            return params;
        }
        for (int i = 0; i < Math.min(args.length, paramNames.length); i++) {
            if (args[i] != null && !isSensitiveParam(paramNames[i])) {
                params.put(paramNames[i], args[i]);
            }
        }
        return params;
    }

    private boolean isSensitiveParam(String paramName) {
        return "password".equalsIgnoreCase(paramName) ||
                "token".equalsIgnoreCase(paramName) ||
                "secret".equalsIgnoreCase(paramName);
    }

    private String serializeParams(Map<String, Object> params) {
        try {
            String json = objectMapper.writeValueAsString(params);
            if (json.length() > 2048) {
                return json.substring(0, 2048);
            }
            return json;
        } catch (Exception e) {
            return "{}";
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
