package com.jinshu.api.dao;

import com.jinshu.common.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    Report selectById(@Param("id") Long id);

    List<Report> selectList(@Param("tenantId") Long tenantId, @Param("name") String name,
                            @Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);

    long countList(@Param("tenantId") Long tenantId, @Param("name") String name, @Param("status") String status);

    int insert(Report report);

    int update(Report report);
}
