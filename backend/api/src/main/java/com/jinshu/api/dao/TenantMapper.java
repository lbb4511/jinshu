package com.jinshu.api.dao;

import com.jinshu.common.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TenantMapper {

    Tenant selectById(@Param("id") Long id);

    Tenant selectByCode(@Param("code") String code);

    List<Tenant> selectList(@Param("name") String name, @Param("status") String status,
                           @Param("offset") int offset, @Param("limit") int limit);

    long countList(@Param("name") String name, @Param("status") String status);

    void insert(Tenant tenant);

    void update(Tenant tenant);
}
