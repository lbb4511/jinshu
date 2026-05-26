package com.jinshu.worker.pdf;

import com.jinshu.common.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PdfTaskMapper {
    Task selectById(@Param("id") Long id);
    void update(Task task);
}
