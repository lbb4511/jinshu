package com.jinshu.batch.dao;

import com.jinshu.common.entity.ImportErrorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImportErrorLogMapper {

    void batchInsert(@Param("list") List<ImportErrorLog> records);

    List<ImportErrorLog> selectByTaskId(@Param("taskId") Long taskId,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    long countByTaskId(@Param("taskId") Long taskId);
}
