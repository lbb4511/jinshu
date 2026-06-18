package com.jinshu.common.security;

/**
 * 敏感字段脱敏规则类型枚举
 *
 * 定义常用的脱敏规则，与 desensitize_rule 表的 rule_type 列对应。
 */
public enum DesensitizeType {

    /**
     * 手机号：保留前 3 位和后 4 位，中间替换为 ****
     */
    PHONE,

    /**
     * 身份证号：保留前 6 位和后 4 位，中间替换为 ********
     */
    ID_CARD,

    /**
     * 银行卡号：保留前 4 位和后 4 位，中间替换为 ******
     */
    BANK_CARD,

    /**
     * 邮箱：保留首字符和域名，中间替换为 ***
     */
    EMAIL,

    /**
     * 姓名：保留首字，其余替换为 *
     */
    NAME,

    /**
     * 金额：按角色分级脱敏，VIEWER 显示为 ***
     */
    AMOUNT,

    /**
     * 全掩码：任何值均显示为 ****
     */
    FULL_MASK
}
