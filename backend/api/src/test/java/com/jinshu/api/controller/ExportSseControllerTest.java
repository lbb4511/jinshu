package com.jinshu.api.controller;

import com.jinshu.api.service.ExportProgressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportSseController - 导出进度 SSE 控制器测试")
class ExportSseControllerTest {

    @Mock
    private ExportProgressService exportProgressService;

    @InjectMocks
    private ExportSseController exportSseController;

    @Test
    @DisplayName("subscribeProgress: 返回 SseEmitter")
    void given_taskId_when_subscribeProgress_then_returnEmitter() {
        Long taskId = 300L;
        SseEmitter expectedEmitter = new SseEmitter();
        when(exportProgressService.subscribe(taskId)).thenReturn(expectedEmitter);

        SseEmitter emitter = exportSseController.subscribeProgress(taskId);

        assertThat(emitter).isEqualTo(expectedEmitter);
    }
}
