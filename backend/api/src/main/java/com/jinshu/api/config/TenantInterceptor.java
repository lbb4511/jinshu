package com.jinshu.api.config;

import com.jinshu.common.context.TenantContext;
import com.jinshu.common.security.SkipTenantFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class TenantInterceptor implements Interceptor {

    private static final String TENANT_FIELD = "tenant_id";
    private static final String TENANT_PLACEHOLDER = "tenant_id = ? AND";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        if (shouldSkip(ms)) {
            return invocation.proceed();
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.debug("No tenant context, skipping tenant filter");
            return invocation.proceed();
        }

        String originalSql = ms.getBoundSql(parameter).getSql();
        String modifiedSql = modifySql(originalSql, tenantId);

        if (modifiedSql.equals(originalSql)) {
            return invocation.proceed();
        }

        BoundSql boundSql = ms.getBoundSql(parameter);
        Configuration configuration = ms.getConfiguration();
        MappedStatement newMs = createNewMappedStatement(ms, new TenantBoundSqlSqlSource(configuration, modifiedSql, boundSql));

        args[0] = newMs;

        return invocation.proceed();
    }

    private boolean shouldSkip(MappedStatement ms) {
        String mapperId = ms.getId();
        if (mapperId == null) {
            return false;
        }

        String interfaceName = mapperId.substring(0, mapperId.lastIndexOf('.'));
        String methodName = mapperId.substring(mapperId.lastIndexOf('.') + 1);

        try {
            Class<?> mapperInterface = Class.forName(interfaceName);
            for (Method method : mapperInterface.getMethods()) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(SkipTenantFilter.class)) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            log.warn("Failed to load mapper interface: {}", interfaceName);
        }
        return false;
    }

    private String modifySql(String originalSql, Long tenantId) {
        String upperSql = originalSql.toUpperCase().trim();

        if (upperSql.startsWith("SELECT") ||
                upperSql.startsWith("UPDATE") ||
                upperSql.startsWith("DELETE")) {

            if (upperSql.contains(TENANT_FIELD.toUpperCase())) {
                return originalSql;
            }

            if (upperSql.matches("(?i).*\\bWHERE\\b.*")) {
                return replaceFirstPattern(originalSql, "(?i)\\bWHERE\\b", "WHERE " + TENANT_PLACEHOLDER + " ");
            } else if (upperSql.startsWith("UPDATE") && upperSql.matches("(?i).*\\bSET\\b.*")) {
                return replaceFirstPattern(originalSql, "(?i)\\bSET\\b", "SET " + TENANT_PLACEHOLDER + " ");
            } else if (upperSql.startsWith("DELETE") && upperSql.matches("(?i).*\\bFROM\\b.*")) {
                return replaceFirstPattern(originalSql, "(?i)\\bFROM\\b", "FROM " + TENANT_PLACEHOLDER + " ");
            }
        }

        return originalSql;
    }

    private String replaceFirstPattern(String input, String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return input.substring(0, matcher.start()) + replacement + input.substring(matcher.end());
        }
        return input;
    }

    private MappedStatement createNewMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(),
                ms.getId(),
                newSqlSource,
                ms.getSqlCommandType()
        );
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(String.join(",", ms.getKeyProperties()));
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        return builder.build();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private static class TenantBoundSqlSqlSource implements SqlSource {
        private final Configuration configuration;
        private final String modifiedSql;
        private final BoundSql originalBoundSql;

        public TenantBoundSqlSqlSource(Configuration configuration, String modifiedSql, BoundSql originalBoundSql) {
            this.configuration = configuration;
            this.modifiedSql = modifiedSql;
            this.originalBoundSql = originalBoundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return new BoundSql(
                    configuration,
                    modifiedSql,
                    originalBoundSql.getParameterMappings(),
                    parameterObject
            );
        }
    }
}
