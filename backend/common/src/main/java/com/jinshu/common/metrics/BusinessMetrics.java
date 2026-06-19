package com.jinshu.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 锦书业务指标埋点。
 *
 * 统一维护导入、导出、PDF 渲染、限流等核心业务 Counter / Timer / Gauge，
 * 所有指标均以 {@code jinshu_} 为前缀，便于 Prometheus 抓取与告警。
 */
@Component
public class BusinessMetrics {

    private static final String TAG_STATUS = "status";
    private static final String TAG_TENANT_ID = "tenant_id";
    private static final String TAG_SCOPE = "scope";
    private static final String TAG_TYPE = "type";

    private final MeterRegistry registry;
    private final Timer importDurationTimer;
    private final Timer exportDurationTimer;
    private final Timer pdfDurationTimer;
    private final ConcurrentHashMap<String, AtomicInteger> activeTaskGauges = new ConcurrentHashMap<>();

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.importDurationTimer = Timer.builder("jinshu_report_import_duration_seconds")
                .description("导入任务执行耗时（秒）")
                .register(registry);
        this.exportDurationTimer = Timer.builder("jinshu_report_export_duration_seconds")
                .description("导出任务执行耗时（秒）")
                .register(registry);
        this.pdfDurationTimer = Timer.builder("jinshu_pdf_generate_duration_seconds")
                .description("PDF 生成任务执行耗时（秒）")
                .register(registry);
    }

    /**
     * 记录导入任务结果。
     *
     * @param status   状态：SUCCESS / FAILED
     * @param tenantId 租户 ID
     */
    public void recordImport(String status, Long tenantId) {
        Counter.builder("jinshu_report_import_total")
                .description("导入任务总次数")
                .tag(TAG_STATUS, status)
                .tag(TAG_TENANT_ID, tenantId == null ? "none" : String.valueOf(tenantId))
                .register(registry)
                .increment();
    }

    /**
     * 记录导出任务结果。
     *
     * @param status   状态：SUCCESS / FAILED
     * @param tenantId 租户 ID
     */
    public void recordExport(String status, Long tenantId) {
        Counter.builder("jinshu_report_export_total")
                .description("导出任务总次数")
                .tag(TAG_STATUS, status)
                .tag(TAG_TENANT_ID, tenantId == null ? "none" : String.valueOf(tenantId))
                .register(registry)
                .increment();
    }

    /**
     * 记录 PDF 生成任务结果。
     *
     * @param status   状态：SUCCESS / FAILED
     * @param tenantId 租户 ID
     */
    public void recordPdf(String status, Long tenantId) {
        Counter.builder("jinshu_pdf_generate_total")
                .description("PDF 生成任务总次数")
                .tag(TAG_STATUS, status)
                .tag(TAG_TENANT_ID, tenantId == null ? "none" : String.valueOf(tenantId))
                .register(registry)
                .increment();
    }

    /**
     * 记录一次导入错误行。
     *
     * @param tenantId 租户 ID
     */
    public void recordImportError(Long tenantId) {
        recordImportErrors(tenantId, 1);
    }

    /**
     * 记录多条导入错误行。
     *
     * @param tenantId 租户 ID
     * @param count    错误行数
     */
    public void recordImportErrors(Long tenantId, long count) {
        Counter.builder("jinshu_report_import_errors_total")
                .description("导入错误行总数")
                .tag(TAG_TENANT_ID, tenantId == null ? "none" : String.valueOf(tenantId))
                .register(registry)
                .increment(count);
    }

    /**
     * 记录导入任务耗时。
     *
     * @param seconds 耗时（秒）
     */
    public void recordImportDuration(double seconds) {
        importDurationTimer.record((long) (seconds * 1_000_000_000.0), TimeUnit.NANOSECONDS);
    }

    /**
     * 记录导出任务耗时。
     *
     * @param seconds 耗时（秒）
     */
    public void recordExportDuration(double seconds) {
        exportDurationTimer.record((long) (seconds * 1_000_000_000.0), TimeUnit.NANOSECONDS);
    }

    /**
     * 记录 PDF 生成任务耗时。
     *
     * @param seconds 耗时（秒）
     */
    public void recordPdfDuration(double seconds) {
        pdfDurationTimer.record((long) (seconds * 1_000_000_000.0), TimeUnit.NANOSECONDS);
    }

    /**
     * 维护活跃任务数 Gauge。
     *
     * @param type     任务类型：IMPORT / EXPORT / PDF 等
     * @param status   任务状态：PENDING / PROCESSING / SUCCESS / FAILED 等
     * @param tenantId 租户 ID
     * @param delta    变化量，正数增加、负数减少
     */
    public void trackActiveTask(String type, String status, Long tenantId, int delta) {
        String tenant = tenantId == null ? "none" : String.valueOf(tenantId);
        String key = type + "|" + status + "|" + tenant;
        AtomicInteger value = activeTaskGauges.computeIfAbsent(key, k -> {
            AtomicInteger ai = new AtomicInteger(0);
            Gauge.builder("jinshu_task_active", ai, AtomicInteger::get)
                    .description("当前活跃任务数")
                    .tag(TAG_TYPE, type)
                    .tag(TAG_STATUS, status)
                    .tag(TAG_TENANT_ID, tenant)
                    .register(registry);
            return ai;
        });
        value.addAndGet(delta);
    }

    /**
     * 记录限流触发次数。
     *
     * @param scope    限流范围：LOGIN_IP / TENANT / USER
     * @param tenantId 租户 ID
     */
    public void recordRateLimitHit(String scope, Long tenantId) {
        Counter.builder("jinshu_rate_limit_hits_total")
                .description("限流触发总次数")
                .tag(TAG_SCOPE, scope)
                .tag(TAG_TENANT_ID, tenantId == null ? "none" : String.valueOf(tenantId))
                .register(registry)
                .increment();
    }
}
