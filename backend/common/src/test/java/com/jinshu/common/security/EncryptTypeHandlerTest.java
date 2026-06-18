package com.jinshu.common.security;

import com.jinshu.common.config.EncryptionProperties;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EncryptTypeHandlerTest {

    private static final String KEY_V1 = "Wi3DdD12Qg1KkvPcS530swxnrzSWCnQN0x3OPCJk8zU=";

    private KeyManager keyManager;
    private EncryptTypeHandler handler;

    @BeforeEach
    void setUp() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setKeys(Map.of(1, KEY_V1));
        properties.setLatestVersion(1);
        keyManager = new KeyManager(properties);
        keyManager.init();
        handler = new EncryptTypeHandler(keyManager);
    }

    @Test
    void shouldEncryptParameterWhenSetting() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        String plaintext = "{\"password\":\"secret\"}";

        handler.setNonNullParameter(ps, 1, plaintext, JdbcType.VARCHAR);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(ps).setString(org.mockito.ArgumentMatchers.eq(1), captor.capture());

        String encrypted = captor.getValue();
        assertNotEquals(plaintext, encrypted);
        assertTrue(encrypted.startsWith("1:"));
    }

    @Test
    void shouldDecryptResultByColumnName() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        String plaintext = "{\"password\":\"secret\"}";
        String encrypted = keyManager.encrypt(plaintext);
        when(rs.getString("connection_config")).thenReturn(encrypted);

        String result = handler.getNullableResult(rs, "connection_config");
        assertEquals(plaintext, result);
    }

    @Test
    void shouldDecryptResultByColumnIndex() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        String plaintext = "plain";
        String encrypted = keyManager.encrypt(plaintext);
        when(rs.getString(3)).thenReturn(encrypted);

        String result = handler.getNullableResult(rs, 3);
        assertEquals(plaintext, result);
    }

    @Test
    void shouldReturnNullWhenResultIsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("connection_config")).thenReturn(null);

        assertNull(handler.getNullableResult(rs, "connection_config"));
    }

    @Test
    void shouldDecryptCallableStatementResult() throws Exception {
        CallableStatement cs = mock(CallableStatement.class);
        String plaintext = "callable";
        String encrypted = keyManager.encrypt(plaintext);
        when(cs.getString(2)).thenReturn(encrypted);

        assertEquals(plaintext, handler.getNullableResult(cs, 2));
    }
}
