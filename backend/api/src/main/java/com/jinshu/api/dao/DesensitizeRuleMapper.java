package com.jinshu.api.dao;

import com.jinshu.common.entity.DesensitizeRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 脱敏规则数据访问接口
 */
@Mapper
public interface DesensitizeRuleMapper {

    /**
     * 按租户、表名、列名查询规则，优先匹配指定租户，未命中时回退到全局规则（tenant_id = 0）
     */
    DesensitizeRule selectByTenantAndField(@Param("tenantId") Long tenantId,
                                           @Param("tableName") String tableName,
                                           @Param("columnName") String columnName);

    /**
     * 查询指定租户的全部规则（不含全局规则）
     */
    List<DesensitizeRule> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 按 ID 查询规则
     */
    DesensitizeRule selectById(@Param("id") Long id);

    /**
     * 新增规则
     */
    int insert(DesensitizeRule rule);

    /**
     * 更新规则
     */
    int update(DesensitizeRule rule);

    /**
     * 删除规则
     */
    int deleteById(@Param("id") Long id);
}
