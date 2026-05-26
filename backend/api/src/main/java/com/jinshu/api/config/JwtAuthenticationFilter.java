package com.jinshu.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.common.context.TenantContext;
import com.jinshu.common.context.UserContext;
import com.jinshu.common.exception.ErrorCode;
import com.jinshu.common.result.Result;
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
import org.springframework.http.MediaType;
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
    private final ObjectMapper objectMapper;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token)) {
                if (!processToken(token, request, response)) {
                    return;
                }
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

    private boolean processToken(String token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Claims claims = jwtTokenProvider.parseToken(token);

            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                return true;
            }

            String jti = claims.getId();
            if (tokenVersionManager.isInBlacklist(jti)) {
                log.warn("Token is in blacklist: {}", jti);
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token已被吊销");
                return false;
            }

            Long userId = Long.parseLong(claims.getSubject());
            Long tenantId = claims.get("tenant_id", Long.class);
            String role = claims.get("role", String.class);
            Integer tokenVersion = claims.get("version", Integer.class);

            if (tokenVersion != null && !tokenVersionManager.isTokenValid(userId, tokenVersion)) {
                log.warn("Token version mismatch for user: {}", userId);
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token已被吊销，请重新登录");
                return false;
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

            return true;

        } catch (ExpiredJwtException e) {
            log.warn("Token expired");
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token已过期，请刷新");
            return false;
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token无效");
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.error(status, message));
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
