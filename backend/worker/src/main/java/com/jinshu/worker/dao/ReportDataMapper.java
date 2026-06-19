package com.jinshu.worker.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

import java.util.Map;

@Mapper
public interface ReportDataMapper {

    /**
     * 根据报表 ID 流式查询报表数据行
     */
    void selectByReportId(@Param("tenantId") Long tenantId,
                          @Param("reportId") Long reportId,
                          ResultHandler<Map<String, Object>> handler);

    /**
     * 统计报表数据行数
     */
    long countByReportId(@Param("tenantId") Long tenantId, @Param("reportId") Long reportId);
}
