package com.jinshu.api.dao;

import com.jinshu.common.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报表数据访问层
 */
@Mapper
public interface ReportMapper {

    Report selectById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    List<Report> selectByReportId(@Param("reportId") Long reportId, @Param("tenantId") Long tenantId);

    List<Report> selectList(@Param("tenantId") Long tenantId);

    int insert(Report report);

    int update(Report report);

    int deleteById(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
