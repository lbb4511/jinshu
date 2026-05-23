package com.jinshu.api.dao;

import com.jinshu.common.entity.ReportWorkflow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportWorkflowMapper {

    ReportWorkflow selectCurrentByReportId(@Param("reportId") Long reportId);

    List<ReportWorkflow> selectByReportId(@Param("reportId") Long reportId);

    void insert(ReportWorkflow workflow);

    void update(ReportWorkflow workflow);
}
