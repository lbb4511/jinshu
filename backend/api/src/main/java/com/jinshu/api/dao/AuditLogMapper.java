package com.jinshu.api.dao;

import com.jinshu.common.entity.AuditLogEntry;
import com.jinshu.common.entity.AuditRootHash;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditLogMapper {

    void insert(AuditLogEntry entry);

    String selectLatestLogHash(@Param("tenantId") Long tenantId);

    void insertRootHash(AuditRootHash rootHash);

    AuditRootHash selectRootHash(@Param("tenantId") Long tenantId,
                                  @Param("hourStart") LocalDateTime hourStart);

    long countByTenantAndTime(@Param("tenantId") Long tenantId,
                               @Param("hourStart") LocalDateTime hourStart);

    List<AuditLogEntry> selectList(@Param("tenantId") Long tenantId,
                                   @Param("userId") Long userId,
                                   @Param("operation") String operation,
                                   @Param("targetType") String targetType,
                                   @Param("status") String status,
                                   @Param("dateFrom") LocalDateTime dateFrom,
                                   @Param("dateTo") LocalDateTime dateTo,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    long countList(@Param("tenantId") Long tenantId,
                   @Param("userId") Long userId,
                   @Param("operation") String operation,
                   @Param("targetType") String targetType,
                   @Param("status") String status,
                   @Param("dateFrom") LocalDateTime dateFrom,
                   @Param("dateTo") LocalDateTime dateTo);

    List<AuditLogEntry> selectByHour(@Param("tenantId") Long tenantId,
                                      @Param("hourStart") LocalDateTime hourStart,
                                      @Param("hourEnd") LocalDateTime hourEnd);

    List<AuditRootHash> selectRootHashList(@Param("tenantId") Long tenantId,
                                            @Param("dateFrom") LocalDateTime dateFrom,
                                            @Param("dateTo") LocalDateTime dateTo);
}
