package com.jinshu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导出任务实时进度视图对象。
 *
 * 用于 Redis 缓存与 SSE 推送，包含任务状态、处理行数、总进度百分比等信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportProgress {

    private Long taskId;

    private String status;

    private Integer progress;

    private Long processedRows;

    private Long totalRows;

    private String message;

    private Long updatedAt;
}
