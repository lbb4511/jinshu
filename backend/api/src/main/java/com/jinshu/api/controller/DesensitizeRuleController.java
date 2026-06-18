package com.jinshu.api.controller;

import com.jinshu.api.service.DesensitizeRuleService;
import com.jinshu.common.entity.DesensitizeRule;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.result.Result;
import com.jinshu.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 脱敏规则管理 API
 *
 * 仅 SECURITY_ADMIN 角色可管理脱敏规则。
 */
@RestController
@RequestMapping("/admin/desensitize-rules")
@RequiredArgsConstructor
@RequireRole("SECURITY_ADMIN")
public class DesensitizeRuleController {

    private final DesensitizeRuleService ruleService;

    @GetMapping
    public Result<PageResult<DesensitizeRule>> listRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(ruleService.listRules(page, pageSize));
    }

    @GetMapping("/all")
    public Result<List<DesensitizeRule>> listAllRules() {
        return Result.success(ruleService.listAllRules());
    }

    @GetMapping("/{id}")
    public Result<DesensitizeRule> getRule(@PathVariable Long id) {
        return Result.success(ruleService.getRule(id));
    }

    @PostMapping
    @RequireRole("SECURITY_ADMIN")
    public Result<DesensitizeRule> createRule(@RequestBody DesensitizeRule rule) {
        return Result.success(ruleService.createRule(rule));
    }

    @PutMapping("/{id}")
    @RequireRole("SECURITY_ADMIN")
    public Result<DesensitizeRule> updateRule(@PathVariable Long id, @RequestBody DesensitizeRule rule) {
        return Result.success(ruleService.updateRule(id, rule));
    }

    @DeleteMapping("/{id}")
    @RequireRole("SECURITY_ADMIN")
    public Result<Void> deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return Result.success(null);
    }

    @PostMapping("/refresh")
    @RequireRole("SECURITY_ADMIN")
    public Result<Void> refreshCache() {
        ruleService.refreshCache();
        return Result.success(null);
    }
}
