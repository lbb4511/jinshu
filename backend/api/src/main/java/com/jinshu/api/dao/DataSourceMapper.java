package com.jinshu.api.dao;

import com.jinshu.common.entity.DataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataSourceMapper {

    DataSource selectById(@Param("id") Long id);

    DataSource selectByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    DataSource selectByNameAndTenantId(@Param("name") String name, @Param("tenantId") Long tenantId);

    List<DataSource> selectList(@Param("tenantId") Long tenantId, @Param("name") String name,
                                @Param("type") String type, @Param("offset") int offset, @Param("limit") int limit);

    long countList(@Param("tenantId") Long tenantId, @Param("name") String name, @Param("type") String type);

    void insert(DataSource dataSource);

    void update(DataSource dataSource);

    long countByDataSourceId(@Param("dataSourceId") Long dataSourceId);
}
