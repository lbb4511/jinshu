package com.jinshu.api.dao;

import com.jinshu.common.entity.RoleChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色变更审计日志 Mapper
 */
@Mapper
public interface RoleChangeLogMapper {

    /**
     * 插入角色变更记录
     *
     * @param roleChangeLog 角色变更日志实体
     */
    void insert(RoleChangeLog roleChangeLog);
}
