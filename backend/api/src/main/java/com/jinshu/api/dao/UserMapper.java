package com.jinshu.api.dao;

import com.jinshu.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User selectByUsername(@Param("username") String username);

    User selectByUsernameAndTenantId(@Param("username") String username, @Param("tenantId") Long tenantId);

    User selectById(@Param("id") Long id);

    List<User> selectList(@Param("tenantId") Long tenantId, @Param("username") String username,
                         @Param("role") String role, @Param("offset") int offset, @Param("limit") int limit);

    long countList(@Param("tenantId") Long tenantId, @Param("username") String username,
                   @Param("role") String role);

    long countByTenantId(@Param("tenantId") Long tenantId);

    void insert(User user);

    void update(User user);
}
