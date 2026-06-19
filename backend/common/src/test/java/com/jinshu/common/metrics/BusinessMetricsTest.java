package com.jinshu.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessMetrics 业务指标埋点测试")
class BusinessMetricsTest {

    private SimpleMeterRegistry registry;
    private BusinessMetrics businessMetrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        businessMetrics = new BusinessMetrics(registry);
    }

    @Test
    @DisplayName("recordImport：按状态与租户记录导入次数")
    void given_importRecords_when_recordImport_then_counterIncreased() {
        businessMetrics.recordImport("SUCCESS", 1L);
        businessMetrics.recordImport("SUCCESS", 1L);
        businessMetrics.recordImport("FAILED", 2L);

        Counter success = registry.find("jinshu_report_import_total")
                .tags("status", "SUCCESS", "tenant_id", "1")
                .counter();
        Counter failed = registry.find("jinshu_report_import_total")
                .tags("status", "FAILED", "tenant_id", "2")
                .counter();

        assertThat(success).isNotNull();
        assertThat(success.count()).isEqualTo(2.0);
        assertThat(failed).isNotNull();
        assertThat(failed.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordExport：按状态与租户记录导出次数")
    void given_exportRecords_when_recordExport_then_counterIncreased() {
        businessMetrics.recordExport("SUCCESS", 10L);
        businessMetrics.recordExport("FAILED", 10L);

        Counter success = registry.find("jinshu_report_export_total")
                .tags("status", "SUCCESS", "tenant_id", "10")
                .counter();
        Counter failed = registry.find("jinshu_report_export_total")
                .tags("status", "FAILED", "tenant_id", "10")
                .counter();

        assertThat(success.count()).isEqualTo(1.0);
        assertThat(failed.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordPdf：按状态与租户记录 PDF 生成次数")
    void given_pdfRecords_when_recordPdf_then_counterIncreased() {
        businessMetrics.recordPdf("SUCCESS", 5L);

        Counter counter = registry.find("jinshu_pdf_generate_total")
                .tags("status", "SUCCESS", "tenant_id", "5")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordImportErrors：按租户记录导入错误行数")
    void given_importErrors_when_recordImportErrors_then_counterIncreased() {
        businessMetrics.recordImportErrors(3L, 7);
        businessMetrics.recordImportError(3L);

        Counter counter = registry.find("jinshu_report_import_errors_total")
                .tags("tenant_id", "3")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(8.0);
    }

    @Test
    @DisplayName("recordImportDuration：记录导入耗时")
    void given_importDuration_when_record_then_timerIncreased() {
        businessMetrics.recordImportDuration(1.5);

        assertThat(registry.get("jinshu_report_import_duration_seconds").timer().count()).isEqualTo(1);
        assertThat(registry.get("jinshu_report_import_duration_seconds").timer().totalTime(java.util.concurrent.TimeUnit.SECONDS))
                .isEqualTo(1.5);
    }

    @Test
    @DisplayName("recordPdfDuration：记录 PDF 耗时")
    void given_pdfDuration_when_record_then_timerIncreased() {
        businessMetrics.recordPdfDuration(0.8);

        assertThat(registry.get("jinshu_pdf_generate_duration_seconds").timer().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("trackActiveTask：维护活跃任务 Gauge")
    void given_activeTasks_when_track_then_gaugeUpdated() {
        businessMetrics.trackActiveTask("IMPORT", "PROCESSING", 1L, 3);
        businessMetrics.trackActiveTask("IMPORT", "PROCESSING", 1L, -1);

        Double value = registry.find("jinshu_task_active")
                .tag("type", "IMPORT")
                .tag("status", "PROCESSING")
                .tag("tenant_id", "1")
                .gauge()
                .value();

        assertThat(value).isEqualTo(2.0);
    }

    @Test
    @DisplayName("recordRateLimitHit：按 scope 与租户记录限流触发")
    void given_rateLimitHit_when_record_then_counterIncreased() {
        businessMetrics.recordRateLimitHit("TENANT", 8L);
        businessMetrics.recordRateLimitHit("USER", 8L);

        Counter tenant = registry.find("jinshu_rate_limit_hits_total")
                .tags("scope", "TENANT", "tenant_id", "8")
                .counter();
        Counter user = registry.find("jinshu_rate_limit_hits_total")
                .tags("scope", "USER", "tenant_id", "8")
                .counter();

        assertThat(tenant.count()).isEqualTo(1.0);
        assertThat(user.count()).isEqualTo(1.0);
    }
}
