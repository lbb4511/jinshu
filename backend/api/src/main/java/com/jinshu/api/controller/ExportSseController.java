package com.jinshu.api.controller;

import com.jinshu.api.service.ExportProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 导出任务实时进度 SSE 控制器。
 *
 * 提供 {@code GET /export/{taskId}/progress/sse} 端点，客户端可通过 EventSource 订阅
 * 导出任务的实时进度、处理行数及终态通知。
 */
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportSseController {

    private final ExportProgressService exportProgressService;

    @GetMapping(value = "/{taskId}/progress/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeProgress(@PathVariable Long taskId) {
        return exportProgressService.subscribe(taskId);
    }
}
