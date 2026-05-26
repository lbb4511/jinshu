package com.jinshu.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.dao.ReportDataMapper;
import com.jinshu.api.dao.ReportMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.entity.Report;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 报表 HTML 预览控制器
 *
 * 功能：根据报表元数据和宽表数据，动态渲染 HTML 表格页面
 * 鉴权：JWT Token（通过 Authorization 请求头）
 */
@Slf4j
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportPreviewController {

    private final ReportMapper reportMapper;
    private final ReportDataMapper reportDataMapper;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    public String previewReport(@PathVariable Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null || Boolean.TRUE.equals(report.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表不存在");
        }

        Long tenantId = TenantContext.getTenantId();
        List<Map<String, Object>> rows = reportDataMapper.selectByReportId(tenantId, id);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"zh-CN\"><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>").append(escapeHtml(report.getName())).append("</title>");
        html.append("<style>");
        html.append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;margin:0;padding:20px;background:#f5f5f5;}");
        html.append(".container{max-width:1200px;margin:0 auto;background:#fff;padding:24px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);}");
        html.append("h1{font-size:22px;color:#1f1f1f;margin:0 0 8px 0;padding-bottom:12px;border-bottom:2px solid #1890ff;}");
        html.append(".desc{color:#666;font-size:14px;margin-bottom:24px;}");
        html.append("table{width:100%;border-collapse:collapse;font-size:14px;line-height:1.5;}");
        html.append("th{background:#fafafa;color:#1f1f1f;font-weight:600;text-align:left;padding:12px 16px;border:1px solid #f0f0f0;white-space:nowrap;}");
        html.append("td{padding:12px 16px;border:1px solid #f0f0f0;color:#333;}");
        html.append("tr:nth-child(even){background:#fafafa;}");
        html.append("tr:hover{background:#e6f7ff;}");
        html.append(".empty{color:#999;text-align:center;padding:48px;font-size:14px;}");
        html.append(".footer{text-align:right;color:#999;font-size:12px;margin-top:24px;padding-top:12px;border-top:1px solid #f0f0f0;}");
        html.append("@media print{body{background:#fff;}.container{box-shadow:none;padding:0;}}");
        html.append("</style></head><body>");
        html.append("<div class=\"container\">");
        html.append("<h1>").append(escapeHtml(report.getName())).append("</h1>");
        if (report.getDescription() != null && !report.getDescription().isEmpty()) {
            html.append("<div class=\"desc\">").append(escapeHtml(report.getDescription())).append("</div>");
        }

        if (rows == null || rows.isEmpty()) {
            html.append("<div class=\"empty\">暂无数据</div>");
        } else {
            try {
                Map<String, Object> firstRow = objectMapper.readValue(
                        (String) rows.get(0).get("data_json_text"), new TypeReference<>() {});

                if (firstRow != null && !firstRow.isEmpty()) {
                    html.append("<table><thead><tr>");
                    for (String key : firstRow.keySet()) {
                        html.append("<th>").append(escapeHtml(key)).append("</th>");
                    }
                    html.append("</tr></thead><tbody>");

                    for (Map<String, Object> rowMap : rows) {
                        String jsonText = (String) rowMap.get("data_json_text");
                        Map<String, Object> dataJson = objectMapper.readValue(jsonText, new TypeReference<>() {});
                        html.append("<tr>");
                        for (Object value : dataJson.values()) {
                            html.append("<td>").append(escapeHtml(value != null ? value.toString() : "")).append("</td>");
                        }
                        html.append("</tr>");
                    }
                    html.append("</tbody></table>");
                } else {
                    html.append("<div class=\"empty\">数据格式异常</div>");
                }
            } catch (Exception e) {
                log.warn("Failed to parse report data JSON", e);
                html.append("<div class=\"empty\">数据解析失败</div>");
            }
        }

        html.append("<div class=\"footer\">锦书企业级报表系统 · 生成时间：")
                .append(java.time.LocalDateTime.now().toString().replace("T", " ").substring(0, 19))
                .append("</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
