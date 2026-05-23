package com.jinshu.api.config;

import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.exception.BusinessException;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.security.JwtTokenProvider;
import com.jinshu.common.security.TokenVersionManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenVersionManager tokenVersionManager;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token)) {
                processToken(token, request);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            UserContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void processToken(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtTokenProvider.parseToken(token);

            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                return;
            }

            String jti = claims.getId();
            if (tokenVersionManager.isInBlacklist(jti)) {
                log.warn("Token is in blacklist: {}", jti);
                throw new BusinessException(ErrorCode.TOKEN_INVALID, "Token已被吊销");
            }

            Long userId = Long.parseLong(claims.getSubject());
            Long tenantId = claims.get("tenant_id", Long.class);
            String role = claims.get("role", String.class);
            Integer tokenVersion = claims.get("version", Integer.class);

            if (tokenVersion != null && !tokenVersionManager.isTokenValid(userId, tokenVersion)) {
                log.warn("Token version mismatch for user: {}", userId);
                throw new BusinessException(ErrorCode.TOKEN_INVALID, "Token已被吊销，请重新登录");
            }

            TenantContext.setTenantId(tenantId);
            UserContext.setUserId(userId);
            UserContext.setUsername(null);
            UserContext.setRole(role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            request.setAttribute("userId", userId);
            request.setAttribute("tenantId", tenantId);
            request.setAttribute("tokenId", jti);

        } catch (ExpiredJwtException e) {
            log.warn("Token expired");
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Token已过期");
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Token无效");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") ||
                path.equals("/health") ||
                path.equals("/ready") ||
                path.equals("/live");
    }
}
