package com.mdd.auth.controller;

import com.mdd.auth.domain.User;
import com.mdd.auth.dto.AuthResponse;
import com.mdd.auth.dto.LoginRequest;
import com.mdd.auth.dto.RefreshTokenRequest;
import com.mdd.auth.dto.RegisterRequest;
import com.mdd.auth.dto.UserResponse;
import com.mdd.auth.service.AuthService;
import com.mdd.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AuthResponse response = authService.register(request, servletRequest);
        setRefreshCookie(servletResponse, response.refreshToken());
        return response.withoutRefreshToken();
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AuthResponse response = authService.login(request, servletRequest);
        setRefreshCookie(servletResponse, response.refreshToken());
        return response.withoutRefreshToken();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AuthResponse response = authService.refresh(resolveRefreshToken(request, servletRequest), servletRequest);
        setRefreshCookie(servletResponse, response.refreshToken());
        return response.withoutRefreshToken();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        authService.logout(resolveRefreshToken(request, servletRequest));
        clearRefreshCookie(servletResponse);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return authService.currentUser(user);
    }

    private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        String cookieToken = refreshCookieValue(servletRequest);
        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        return request == null ? null : request.refreshToken();
    }

    private String refreshCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (jwtProperties.refreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = refreshCookie(refreshToken, Duration.ofMillis(jwtProperties.refreshExpiration()));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = refreshCookie("", Duration.ZERO);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(jwtProperties.refreshCookieName(), value)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite(jwtProperties.refreshCookieSameSite())
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
