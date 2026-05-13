package com.jinshu.common.context;

/**
 * 用户上下文工具类
 *
 * 基于ThreadLocal存储当前登录用户信息
 *
 * 使用场景：
 * - 登录拦截器提取用户信息存储
 * - 审计日志记录操作人
 * - 业务层获取当前用户
 *
 * 注意事项：
 * - 必须在请求结束时调用clear()防止内存泄漏
 */
public class UserContext {

    /**
     * 用户ID存储
     */
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 用户名存储
     */
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();

    /**
     * 私有构造函数，防止实例化
     */
    private UserContext() {
    }

    /**
     * 设置当前用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 设置当前用户名
     *
     * @param username 用户名
     */
    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名
     */
    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    /**
     * 清除当前用户信息
     *
     * 防止内存泄漏，必须在请求结束时调用
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
    }
}
