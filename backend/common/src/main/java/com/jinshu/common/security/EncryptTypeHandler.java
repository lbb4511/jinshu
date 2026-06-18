package com.jinshu.common.security;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 敏感字段加密 MyBatis TypeHandler
 *
 * 写入数据库时自动加密，读取时自动解密。
 * 配合 &#064;EncryptField 注解与 MyBatis XML 中的 typeHandler 声明使用。
 *
 * 注意：该 TypeHandler 由 Spring 容器创建并注入 KeyManager，
 * 因此需在 MyBatis 配置中注册为 Spring Bean。
 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(String.class)
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    private final KeyManager keyManager;

    public EncryptTypeHandler(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, keyManager.encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decryptIfEncrypted(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decryptIfEncrypted(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decryptIfEncrypted(cs.getString(columnIndex));
    }

    private String decryptIfEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        // 兼容历史明文数据：非密文格式时原样返回，首次写入时自动加密
        return keyManager.isEncrypted(value) ? keyManager.decrypt(value) : value;
    }
}
