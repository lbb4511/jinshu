package com.jinshu.common.dao;

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

    List<ImportErrorLog> selectByTaskIdAndRowNo(@Param("taskId") Long taskId,
                                                @Param("rowNo") Integer rowNo,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    long countByTaskIdAndRowNo(@Param("taskId") Long taskId,
                               @Param("rowNo") Integer rowNo);

    void deleteByTaskId(@Param("taskId") Long taskId);
}
