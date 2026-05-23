package com.jinshu.api.dao;

import com.jinshu.common.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMapper {

    Task selectById(@Param("id") Long id);

    List<Task> selectList(@Param("tenantId") Long tenantId, @Param("status") String status,
                         @Param("offset") int offset, @Param("limit") int limit);

    long countList(@Param("tenantId") Long tenantId, @Param("status") String status);

    void insert(Task task);

    void update(Task task);

    long countByStatus(@Param("tenantId") Long tenantId, @Param("status") String status);
}
