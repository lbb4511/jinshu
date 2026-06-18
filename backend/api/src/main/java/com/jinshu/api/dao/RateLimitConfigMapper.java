package com.jinshu.api.dao;

import com.jinshu.common.entity.RateLimitConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RateLimitConfigMapper {

    /**
     * 查询指定租户级的限流规则
     *
     * @param scope    限流维度
     * @param tenantId 租户 ID
     * @return 租户级限流规则，不存在返回 null
     */
    RateLimitConfig selectByScopeAndTenant(@Param("scope") String scope,
                                           @Param("tenantId") Long tenantId);

    /**
     * 查询指定维度的全局限流规则
     *
     * @param scope 限流维度
     * @return 全局规则，不存在返回 null
     */
    RateLimitConfig selectGlobalByScope(@Param("scope") String scope);
}
