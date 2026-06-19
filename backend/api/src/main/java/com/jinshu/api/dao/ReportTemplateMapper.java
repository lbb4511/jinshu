package com.jinshu.api.dao;

import com.jinshu.common.entity.ReportTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportTemplateMapper {

    int insert(ReportTemplate template);

    int update(ReportTemplate template);

    int deleteById(@Param("id") Long id);

    ReportTemplate selectById(@Param("id") Long id);

    List<ReportTemplate> selectList(@Param("tenantId") Long tenantId,
                                    @Param("category") String category,
                                    @Param("status") String status,
                                    @Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    long countList(@Param("tenantId") Long tenantId,
                   @Param("category") String category,
                   @Param("status") String status,
                   @Param("keyword") String keyword);

    List<ReportTemplate> selectPublicTemplates(@Param("tenantId") Long tenantId,
                                               @Param("category") String category,
                                               @Param("status") String status,
                                               @Param("keyword") String keyword,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    long countPublicTemplates(@Param("tenantId") Long tenantId,
                              @Param("category") String category,
                              @Param("status") String status,
                              @Param("keyword") String keyword);
}
