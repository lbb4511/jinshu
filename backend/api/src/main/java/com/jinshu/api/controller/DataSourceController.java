package com.jinshu.api.controller;

import com.jinshu.api.service.DataSourceService;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.entity.DataSource;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'REPORT_DESIGNER')")
    @AuditLog(operation = "CREATE_DATASOURCE", targetType = "DATASOURCE")
    public Result<DataSource> createDataSource(@RequestBody DataSourceService.CreateDataSourceRequest request) {
        DataSource dataSource = dataSourceService.createDataSource(request);
        return Result.success(dataSource);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'REPORT_DESIGNER', 'REVIEWER', 'VIEWER')")
    public Result<PageResult<DataSource>> listDataSources(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<DataSource> result = dataSourceService.listDataSources(name, type, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'REPORT_DESIGNER', 'REVIEWER', 'VIEWER')")
    public Result<DataSource> getDataSource(@PathVariable Long id) {
        DataSource dataSource = dataSourceService.getDataSourceById(id);
        return Result.success(dataSource);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'REPORT_DESIGNER')")
    @AuditLog(operation = "UPDATE_DATASOURCE", targetType = "DATASOURCE")
    public Result<DataSource> updateDataSource(@PathVariable Long id,
                                               @RequestBody DataSourceService.UpdateDataSourceRequest request) {
        DataSource dataSource = dataSourceService.updateDataSource(id, request);
        return Result.success(dataSource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    @AuditLog(operation = "DELETE_DATASOURCE", targetType = "DATASOURCE")
    public Result<Void> deleteDataSource(@PathVariable Long id) {
        dataSourceService.deleteDataSource(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'REPORT_DESIGNER')")
    public Result<Map<String, Object>> testConnection(@PathVariable Long id) {
        Map<String, Object> result = dataSourceService.testConnection(id);
        return Result.success(result);
    }
}
