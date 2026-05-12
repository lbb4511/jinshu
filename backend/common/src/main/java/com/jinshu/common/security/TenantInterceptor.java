package com.jinshu.common.security;

import com.jinshu.common.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Properties;

@Slf4j
@Component
@Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
public class TenantInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        SkipTenantFilter skipAnnotation = getSkipTenantFilterAnnotation(mappedStatement);
        if (skipAnnotation != null) {
            return invocation.proceed();
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return invocation.proceed();
        }

        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        String sqlWithTenant = addTenantCondition(originalSql, tenantId, boundSql);

        metaObject.setValue("delegate.boundSql.sql", sqlWithTenant);

        return invocation.proceed();
    }

    private SkipTenantFilter getSkipTenantFilterAnnotation(MappedStatement mappedStatement) throws ClassNotFoundException {
        String namespace = mappedStatement.getId().substring(0, mappedStatement.getId().lastIndexOf('.'));
        String methodName = mappedStatement.getId().substring(mappedStatement.getId().lastIndexOf('.') + 1);

        Class<?> mapperClass = Class.forName(namespace);

        for (java.lang.reflect.Method method : mapperClass.getMethods()) {
            if (method.getName().equals(methodName) && method.isAnnotationPresent(SkipTenantFilter.class)) {
                return method.getAnnotation(SkipTenantFilter.class);
            }
        }

        if (mapperClass.isAnnotationPresent(SkipTenantFilter.class)) {
            return mapperClass.getAnnotation(SkipTenantFilter.class);
        }

        return null;
    }

    private String addTenantCondition(String sql, Long tenantId, BoundSql boundSql) {
        String lowerSql = sql.toLowerCase().trim();

        if (lowerSql.startsWith("insert")) {
            return sql;
        }

        // 将 tenantId 添加到 MyBatis 参数中，避免 SQL 注入
        boundSql.setAdditionalParameter("_tenantId", tenantId);

        if (lowerSql.contains("where")) {
            return sql.replaceFirst("(?i)where", "WHERE tenant_id = #{_tenantId} AND");
        } else if (lowerSql.contains("group by") || lowerSql.contains("order by") || lowerSql.contains("limit")) {
            int index = sql.toLowerCase().indexOf(" group by ");
            if (index == -1) index = sql.toLowerCase().indexOf(" order by ");
            if (index == -1) index = sql.toLowerCase().indexOf(" limit ");

            return sql.substring(0, index) + " WHERE tenant_id = #{_tenantId} " + sql.substring(index);
        } else {
            return sql + " WHERE tenant_id = #{_tenantId}";
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
