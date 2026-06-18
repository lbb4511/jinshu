package com.jinshu.common.security;

import com.jinshu.common.context.UserContext;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * 权限检查工具类
 *
 * 提供编程式角色、资源所有权、审查人权限校验方法。
 * 所有方法均从 {@link UserContext} 获取当前用户角色与 ID。
 */
public class PermissionUtils {

    private PermissionUtils() {
    }

    /**
     * 检查当前用户是否拥有指定角色之一
     *
     * @param allowedRoles 允许的角色列表
     * @throws BusinessException 角色不满足时抛出 403（错误码 20001）
     */
    public static void checkRole(String... allowedRoles) {
        String role = UserContext.getRole();
        if (role == null || !Set.of(allowedRoles).contains(role)) {
            throw new BusinessException(ErrorCode.ROLE_NOT_ALLOWED, "当前角色无权限执行此操作");
        }
    }

    /**
     * 检查当前用户是否为资源所有者
     *
     * ADMIN 角色自动放行；否则要求 resourceOwnerId 等于当前用户 ID。
     *
     * @param resourceOwnerId 资源所有者用户 ID
     * @throws BusinessException 非所有者时抛出 403（错误码 20002）
     */
    public static void checkOwner(Long resourceOwnerId) {
        if (Role.ADMIN.name().equals(UserContext.getRole())) {
            return;
        }
        if (!Objects.equals(UserContext.getUserId(), resourceOwnerId)) {
            throw new BusinessException(ErrorCode.NOT_RESOURCE_OWNER, "仅资源所有者可执行此操作");
        }
    }

    /**
     * 检查当前用户是否为指派的审查人
     *
     * ADMIN 角色自动放行；否则要求 reviewerId 等于当前用户 ID。
     *
     * @param reviewerId 指派的审查人用户 ID
     * @throws BusinessException 非指派审查人时抛出 403（错误码 20003）
     */
    public static void checkReviewer(Long reviewerId) {
        if (Role.ADMIN.name().equals(UserContext.getRole())) {
            return;
        }
        if (!Objects.equals(UserContext.getUserId(), reviewerId)) {
            throw new BusinessException(ErrorCode.NOT_ASSIGNED_REVIEWER, "仅指派的审查人可审批此报表");
        }
    }

    /**
     * 检查当前用户是否拥有指定角色之一（使用 Role 枚举）
     *
     * @param allowedRoles 允许的角色枚举数组
     * @throws BusinessException 角色不满足时抛出 403（错误码 20001）
     */
    public static void checkRole(Role... allowedRoles) {
        String[] names = Arrays.stream(allowedRoles)
                .map(Role::name)
                .toArray(String[]::new);
        checkRole(names);
    }
}
