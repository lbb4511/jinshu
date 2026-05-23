package com.jinshu.api.controller;

import com.jinshu.api.service.AuthService;
import com.jinshu.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        return Result.success(authService.authenticate(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody RefreshRequest request) {
        return Result.success(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return Result.success(null);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.success(authService.getCurrentUser());
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RefreshRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
