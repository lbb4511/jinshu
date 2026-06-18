package com.jinshu.common.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统角色枚举
 *
 * 定义锦书系统五角色体系，与权限控制矩阵一一对应。
 *
 * @see <a href="docs/03-安全与合规/06.权限控制矩阵.md">权限控制矩阵</a>
 */
public enum Role {

    /**
     * 平台管理员：全局超级权限，可管理租户、用户、所有报表
     */
    ADMIN,

    /**
     * 安全管理员：负责安全策略配置，如加密密钥、脱敏规则
     */
    SECURITY_ADMIN,

    /**
     * 审计员：只读所有审计日志，不可操作业务数据
     */
    AUDITOR,

    /**
     * 普通用户：创建/编辑自己的报表，提交审查，导出
     */
    USER,

    /**
     * 访客：仅查看已公开的报表，不可编辑、导出
     */
    VIEWER;

    private static final Set<String> ROLE_NAMES = Arrays.stream(values())
            .map(Role::name)
            .collect(Collectors.toSet());

    /**
     * 判断给定字符串是否为有效的角色标识
     *
     * @param role 角色字符串
     * @return 是否有效
     */
    public static boolean isValid(String role) {
        return role != null && ROLE_NAMES.contains(role);
    }
}
