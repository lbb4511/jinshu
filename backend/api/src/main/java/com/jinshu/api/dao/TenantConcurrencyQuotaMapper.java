package com.jinshu.api.dao;

import com.jinshu.common.entity.TenantConcurrencyQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantConcurrencyQuotaMapper {

    /**
     * 查询租户并发配额
     *
     * @param tenantId 租户 ID
     * @return 租户并发配额，不存在返回 null
     */
    TenantConcurrencyQuota selectByTenantId(@Param("tenantId") Long tenantId);
}
