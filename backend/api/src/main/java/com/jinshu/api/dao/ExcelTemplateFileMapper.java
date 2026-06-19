package com.jinshu.api.dao;

import com.jinshu.common.entity.ExcelTemplateFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExcelTemplateFileMapper {

    int insert(ExcelTemplateFile record);

    int deleteById(@Param("id") Long id);

    ExcelTemplateFile selectById(@Param("id") Long id);

    List<ExcelTemplateFile> selectList(@Param("tenantId") Long tenantId,
                                       @Param("reportId") Long reportId,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    long countList(@Param("tenantId") Long tenantId, @Param("reportId") Long reportId);
}
