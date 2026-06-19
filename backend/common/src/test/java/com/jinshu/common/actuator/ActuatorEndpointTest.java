package com.jinshu.common.actuator;

import com.jinshu.common.CommonTestApplication;
import com.jinshu.common.metrics.BusinessMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Actuator 端点集成测试")
@ActiveProfiles("test")
@SpringBootTest(classes = CommonTestApplication.class)
class ActuatorEndpointTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BusinessMetrics businessMetrics;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("/actuator/health 返回 UP")
    void when_callHealth_then_returnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"UP\"")));
    }

    @Test
    @DisplayName("/actuator/prometheus 返回 200 并包含 jinshu_ 指标")
    void when_callPrometheus_then_returnsJinshuMetrics() throws Exception {
        businessMetrics.recordImport("SUCCESS", 42L);
        businessMetrics.recordExport("SUCCESS", 42L);
        businessMetrics.recordPdf("SUCCESS", 42L);
        businessMetrics.recordImportError(42L);
        businessMetrics.recordRateLimitHit("TENANT", 42L);
        businessMetrics.trackActiveTask("IMPORT", "PROCESSING", 42L, 1);

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jinshu_report_import_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jinshu_report_export_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jinshu_pdf_generate_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jinshu_report_import_errors_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jinshu_rate_limit_hits_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jinshu_task_active")));
    }

    @Test
    @DisplayName("/actuator/info 返回 200")
    void when_callInfo_then_returnsOk() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }
}
