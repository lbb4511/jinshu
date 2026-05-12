package com.jinshu.api.dao;

import com.jinshu.common.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    Report selectById(@Param("id") Long id);

    List<Report> selectByReportId(@Param("reportId") Long reportId);

    List<Report> selectList();

    int insert(Report report);

    int update(Report report);

    int deleteById(@Param("id") Long id);
}
