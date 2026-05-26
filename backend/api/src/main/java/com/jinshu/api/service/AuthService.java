package com.jinshu.api.service;

import com.jinshu.api.dao.TenantMapper;
import com.jinshu.api.dao.UserMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.entity.Tenant;
import com.jinshu.common.entity.User;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.security.JwtTokenProvider;
import com.jinshu.common.security.TokenVersionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenVersionManager tokenVersionManager;

    @Transactional
    public Map<String, Object> authenticate(String username, String password) {
        long failCount = tokenVersionManager.getLoginFailCount(username);
        if (failCount >= 5) {
            long expireSeconds = tokenVersionManager.getLoginFailExpireTime(username);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "账号已锁定，请" + (expireSeconds / 60) + "分钟后重试");
        }

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            tokenVersionManager.incrementLoginFailCount(username);
            throw new BusinessException(ErrorCode.USERNAME_PASSWORD_ERROR);
        }

        if ("DISABLED".equals(user.getStatus())) {
            log.warn("Login attempt for disabled user: {}", username);
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            tokenVersionManager.incrementLoginFailCount(username);
            long remainingFails = 5 - tokenVersionManager.getLoginFailCount(username);
            throw new BusinessException(ErrorCode.USERNAME_PASSWORD_ERROR);
        }

        tokenVersionManager.clearLoginFailCount(username);

        Tenant tenant = tenantMapper.selectById(user.getTenantId());
        if (tenant == null || "DISABLED".equals(tenant.getStatus())) {
            log.warn("Login attempt for disabled tenant: {}", user.getTenantId());
            throw new BusinessException(ErrorCode.TENANT_DISABLED);
        }

        int tokenVersion = (int) tokenVersionManager.getCurrentVersion(user.getId());
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getTenantId(), user.getRole(), tokenVersion);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getTenantId());

        String refreshTokenId = jwtTokenProvider.getTokenIdFromToken(refreshToken);
        long refreshExpiration = jwtTokenProvider.getRefreshTokenExpiration();
        tokenVersionManager.storeRefreshToken(user.getId(), refreshTokenId, refreshExpiration);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenProvider.getAccessTokenExpiration() / 1000);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("tenantId", user.getTenantId());
        userInfo.put("role", user.getRole());
        result.put("user", userInfo);

        log.info("User logged in successfully: {}", username);

        return result;
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String type = jwtTokenProvider.getTypeFromToken(refreshToken);
        if (!"refresh".equals(type)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Long tenantId = jwtTokenProvider.getTenantIdFromToken(refreshToken);
        String tokenId = jwtTokenProvider.getTokenIdFromToken(refreshToken);

        if (!tokenVersionManager.isRefreshTokenValid(userId, tokenId)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        tokenVersionManager.deleteRefreshToken(userId, tokenId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        int tokenVersion = (int) tokenVersionManager.getCurrentVersion(userId);
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, tenantId, user.getRole(), tokenVersion);

        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, tenantId);
        String newRefreshTokenId = jwtTokenProvider.getTokenIdFromToken(newRefreshToken);
        long refreshExpiration = jwtTokenProvider.getRefreshTokenExpiration();
        tokenVersionManager.storeRefreshToken(userId, newRefreshTokenId, refreshExpiration);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenProvider.getAccessTokenExpiration() / 1000);

        return result;
    }

    public void logout(String accessToken) {
        try {
            if (jwtTokenProvider.validateToken(accessToken)) {
                String tokenId = jwtTokenProvider.getTokenIdFromToken(accessToken);
                Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

                long remainingExpiration = jwtTokenProvider.getExpirationFromToken(accessToken) - System.currentTimeMillis();
                tokenVersionManager.addToBlacklist(tokenId, remainingExpiration);

                tokenVersionManager.incrementTokenVersion(userId);
            }
        } catch (Exception e) {
            log.warn("Error during logout: {}", e.getMessage());
        }
    }

    public Map<String, Object> getCurrentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());
        result.put("tenantId", user.getTenantId());

        return result;
    }
}
