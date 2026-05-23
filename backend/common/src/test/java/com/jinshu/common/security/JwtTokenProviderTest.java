package com.jinshu.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider JWT 令牌组件测试")
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "ThisIsATestSecretKeyForJwtTokenProviderUnitTest123456789012345678";
    private static final Long USER_ID = 42L;
    private static final Long TENANT_ID = 100L;
    private static final String ROLE = "ADMIN";

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET);
    }

    @Test
    @DisplayName("创建 Access Token：内容包含 userId, tenantId, role")
    void given_userInfo_when_createAccessToken_then_containsAllClaims() {
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);

        Claims claims = tokenProvider.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(claims.get("tenant_id", Long.class)).isEqualTo(TENANT_ID);
        assertThat(claims.get("role", String.class)).isEqualTo(ROLE);
        assertThat(claims.get("type", String.class)).isEqualTo("access");
    }

    @Test
    @DisplayName("创建 Refresh Token：内容包含 userId, tenantId")
    void given_userInfo_when_createRefreshToken_then_containsCorrectClaims() {
        String token = tokenProvider.createRefreshToken(USER_ID, TENANT_ID);

        Claims claims = tokenProvider.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(claims.get("tenant_id", Long.class)).isEqualTo(TENANT_ID);
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("Access Token 到期时间：30 分钟")
    void given_accessToken_when_checkExpiration_then_30min() {
        assertThat(tokenProvider.getAccessTokenExpiration()).isEqualTo(30 * 60 * 1000L);
    }

    @Test
    @DisplayName("Refresh Token 到期时间：7 天")
    void given_refreshToken_when_checkExpiration_then_7days() {
        assertThat(tokenProvider.getRefreshTokenExpiration()).isEqualTo(7 * 24 * 60 * 60 * 1000L);
    }

    @Test
    @DisplayName("parseToken：解析 Token 返回正确 Claims")
    void given_validToken_when_parse_then_returnClaims() {
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        Claims claims = tokenProvider.parseToken(token);

        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("validateToken：有效 Token 返回 true")
    void given_validToken_when_validate_then_true() {
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken：篡改 Token 返回 false")
    void given_tamperedToken_when_validate_then_false() {
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE) + "tampered";
        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken：随机非法字符串返回 false")
    void given_randomString_when_validate_then_false() {
        assertThat(tokenProvider.validateToken("this.is.not.a.jwt.token")).isFalse();
    }

    @Test
    @DisplayName("parseToken：篡改 Token 抛出 JwtException")
    void given_tamperedToken_when_parse_then_throw() {
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE) + "tampered";
        assertThatThrownBy(() -> tokenProvider.parseToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("getUserIdFromToken：从 Token 中提取 userId")
    void given_token_when_getUserId_then_correct() {
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("getTenantIdFromToken：从 Token 中提取 tenantId")
    void given_token_when_getTenantId_then_correct() {
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        assertThat(tokenProvider.getTenantIdFromToken(token)).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("getRoleFromToken：从 Token 中提取 role")
    void given_token_when_getRole_then_correct() {
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo(ROLE);
    }

    @Test
    @DisplayName("getTokenIdFromToken：Token 包含唯一 ID (jti)")
    void given_token_when_getTokenId_then_notNull() {
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        assertThat(tokenProvider.getTokenIdFromToken(token)).isNotBlank();
    }

    @Test
    @DisplayName("getTypeFromToken：区分 access 和 refresh")
    void given_accessAndRefreshTokens_when_getType_then_different() {
        String accessToken = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        String refreshToken = tokenProvider.createRefreshToken(USER_ID, TENANT_ID);

        assertThat(tokenProvider.getTypeFromToken(accessToken)).isEqualTo("access");
        assertThat(tokenProvider.getTypeFromToken(refreshToken)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("getExpirationFromToken：返回到期时间戳")
    void given_token_when_getExpiration_then_future() {
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        long expiration = tokenProvider.getExpirationFromToken(token);
        assertThat(expiration).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    @DisplayName("不同用户生成的 Token 不同")
    void given_differentUsers_when_createTokens_then_different() {
        String token1 = tokenProvider.createAccessToken(1L, TENANT_ID, "USER");
        String token2 = tokenProvider.createAccessToken(2L, TENANT_ID, "USER");
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("同一用户每次生成的 Token 不同（包含 jti）")
    void given_sameUserTwice_when_createTokens_then_different() {
        String token1 = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        String token2 = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("不同 secret 生成的 Token 无法互相解析")
    void given_differentSecret_when_parse_then_throw() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(TEST_SECRET + "different");
        String token = tokenProvider.createAccessToken(USER_ID, TENANT_ID, ROLE);
        assertThatThrownBy(() -> otherProvider.parseToken(token))
                .isInstanceOf(JwtException.class);
    }
}
