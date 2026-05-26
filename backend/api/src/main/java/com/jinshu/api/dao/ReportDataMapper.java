package com.jinshu.api.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportDataMapper {

    /**
     * 根据报表ID查询报表数据行
     */
    List<Map<String, Object>> selectByReportId(@Param("tenantId") Long tenantId, @Param("reportId") Long reportId);
}
