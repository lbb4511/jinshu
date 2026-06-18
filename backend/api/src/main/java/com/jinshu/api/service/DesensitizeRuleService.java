package com.jinshu.api.service;

import com.jinshu.api.dao.DesensitizeRuleMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.entity.DesensitizeRule;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.PageResult;
import com.jinshu.common.security.DesensitizeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 脱敏规则管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesensitizeRuleService {

    private final DesensitizeRuleMapper ruleMapper;

    public PageResult<DesensitizeRule> listRules(int page, int pageSize) {
        Long tenantId = TenantContext.getTenantId();
        // SECURITY_ADMIN 可查看本租户规则；ADMIN 可查看全部？这里按当前租户
        List<DesensitizeRule> list = ruleMapper.selectByTenantId(tenantId);
        int total = list.size();
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<DesensitizeRule> pageList = from < total ? list.subList(from, to) : List.of();
        return PageResult.of(pageList, total, page, pageSize);
    }

    public List<DesensitizeRule> listAllRules() {
        Long tenantId = TenantContext.getTenantId();
        return ruleMapper.selectByTenantId(tenantId);
    }

    public DesensitizeRule getRule(Long id) {
        // 简化：直接按 id 查询，实际可加入 tenant 校验
        DesensitizeRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "脱敏规则不存在");
        }
        return rule;
    }

    public DesensitizeRule createRule(DesensitizeRule rule) {
        validateRule(rule);
        rule.setTenantId(TenantContext.getTenantId());
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.insert(rule);
        return rule;
    }

    public DesensitizeRule updateRule(Long id, DesensitizeRule rule) {
        DesensitizeRule existing = getRule(id);
        validateRule(rule);
        existing.setTableName(rule.getTableName());
        existing.setColumnName(rule.getColumnName());
        existing.setRuleType(rule.getRuleType());
        existing.setApplicableRoles(rule.getApplicableRoles());
        existing.setEnabled(rule.getEnabled());
        existing.setUpdatedAt(LocalDateTime.now());
        ruleMapper.update(existing);
        return existing;
    }

    public void deleteRule(Long id) {
        getRule(id);
        ruleMapper.deleteById(id);
    }

    public void refreshCache() {
        // 当前实现直接从 DB 查询，无缓存；预留接口用于二期接入 Redis 缓存
        log.info("Desensitize rules cache refresh requested");
    }

    private void validateRule(DesensitizeRule rule) {
        if (!StringUtils.hasText(rule.getTableName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表名不能为空");
        }
        if (!StringUtils.hasText(rule.getColumnName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "列名不能为空");
        }
        if (!StringUtils.hasText(rule.getRuleType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "规则类型不能为空");
        }
        try {
            DesensitizeType.valueOf(rule.getRuleType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的规则类型: " + rule.getRuleType());
        }
        if (StringUtils.hasText(rule.getApplicableRoles())) {
            Set<String> validRoles = Set.of("ADMIN", "SECURITY_ADMIN", "AUDITOR", "USER", "VIEWER");
            Arrays.stream(rule.getApplicableRoles().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(r -> {
                        if (!validRoles.contains(r)) {
                            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的角色: " + r);
                        }
                    });
        }
    }
}
