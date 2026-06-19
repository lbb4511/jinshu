package com.jinshu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 导出任务进度变更事件。
 *
 * Worker 通过 Redis Pub/Sub 发布，API 服务订阅后推送给 SSE 客户端。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportProgressEvent implements Serializable {

    private Long taskId;

    private String status;

    private Integer progress;

    private Long processedRows;

    private Long totalRows;

    private String message;

    private Long timestamp;
}
