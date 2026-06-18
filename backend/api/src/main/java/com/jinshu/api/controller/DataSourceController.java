package com.jinshu.api.controller;

import com.jinshu.api.service.DataSourceService;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.entity.DataSource;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import com.jinshu.common.security.RequireOwner;
import com.jinshu.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @PostMapping
    @RequireRole({"ADMIN", "SECURITY_ADMIN", "USER"})
    @AuditLog(operation = "CREATE_DATASOURCE", targetType = "DATASOURCE")
    public Result<DataSource> createDataSource(@RequestBody DataSourceService.CreateDataSourceRequest request) {
        DataSource dataSource = dataSourceService.createDataSource(request);
        return Result.success(dataSource);
    }

    @GetMapping
    @RequireRole({"ADMIN", "SECURITY_ADMIN", "USER"})
    public Result<PageResult<DataSource>> listDataSources(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<DataSource> result = dataSourceService.listDataSources(name, type, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "SECURITY_ADMIN", "USER"})
    public Result<DataSource> getDataSource(@PathVariable Long id) {
        DataSource dataSource = dataSourceService.getDataSourceById(id);
        return Result.success(dataSource);
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "SECURITY_ADMIN", "USER"})
    @RequireOwner(resourceIdParam = "id")
    @AuditLog(operation = "UPDATE_DATASOURCE", targetType = "DATASOURCE")
    public Result<DataSource> updateDataSource(@PathVariable Long id,
                                               @RequestBody DataSourceService.UpdateDataSourceRequest request) {
        DataSource dataSource = dataSourceService.updateDataSource(id, request);
        return Result.success(dataSource);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "SECURITY_ADMIN", "USER"})
    @RequireOwner(resourceIdParam = "id")
    @AuditLog(operation = "DELETE_DATASOURCE", targetType = "DATASOURCE")
    public Result<Void> deleteDataSource(@PathVariable Long id) {
        dataSourceService.deleteDataSource(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/test")
    @RequireRole({"ADMIN", "SECURITY_ADMIN", "USER"})
    @RequireOwner(resourceIdParam = "id")
    public Result<Map<String, Object>> testConnection(@PathVariable Long id) {
        Map<String, Object> result = dataSourceService.testConnection(id);
        return Result.success(result);
    }
}
