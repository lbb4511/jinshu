package com.jinshu.batch.dao;

import com.jinshu.common.entity.ReportData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportDataMapper {

    void batchInsert(@Param("list") List<ReportData> records);

    void deleteByReportId(@Param("tenantId") Long tenantId, @Param("reportId") Long reportId);

    int countByReportId(@Param("tenantId") Long tenantId, @Param("reportId") Long reportId);
}
