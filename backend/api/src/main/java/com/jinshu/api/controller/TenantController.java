package com.jinshu.api.controller;

import com.jinshu.api.service.TenantService;
import com.jinshu.common.audit.AuditLog;
import com.jinshu.common.entity.Tenant;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import com.jinshu.common.security.SkipTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/admin/tenants")
    @SkipTenantFilter
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(operation = "CREATE_TENANT", targetType = "TENANT")
    public Result<Tenant> createTenant(@RequestBody TenantService.CreateTenantRequest request) {
        Tenant tenant = tenantService.createTenant(request);
        return Result.success(tenant);
    }

    @GetMapping("/admin/tenants")
    @SkipTenantFilter
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<Tenant>> listTenants(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<Tenant> result = tenantService.listTenants(name, status, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/admin/tenants/{id}")
    @SkipTenantFilter
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Tenant> getTenant(@PathVariable Long id) {
        Tenant tenant = tenantService.getTenantById(id);
        return Result.success(tenant);
    }

    @PutMapping("/admin/tenants/{id}")
    @SkipTenantFilter
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(operation = "UPDATE_TENANT", targetType = "TENANT")
    public Result<Tenant> updateTenant(@PathVariable Long id, @RequestBody TenantService.UpdateTenantRequest request) {
        Tenant tenant = tenantService.updateTenant(id, request);
        return Result.success(tenant);
    }

    @PatchMapping("/admin/tenants/{id}/status")
    @SkipTenantFilter
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(operation = "CHANGE_TENANT_STATUS", targetType = "TENANT")
    public Result<Tenant> changeTenantStatus(@PathVariable Long id, @RequestParam String status) {
        Tenant tenant = tenantService.changeTenantStatus(id, status);
        return Result.success(tenant);
    }

    @DeleteMapping("/admin/tenants/{id}")
    @SkipTenantFilter
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(operation = "ARCHIVE_TENANT", targetType = "TENANT")
    public Result<Void> archiveTenant(@PathVariable Long id) {
        tenantService.archiveTenant(id);
        return Result.success(null);
    }

    @GetMapping("/tenant/quota")
    public Result<Map<String, Object>> getQuota() {
        Long tenantId = 1L;
        Map<String, Object> quota = tenantService.getQuotaUsage(tenantId);
        return Result.success(quota);
    }
}
