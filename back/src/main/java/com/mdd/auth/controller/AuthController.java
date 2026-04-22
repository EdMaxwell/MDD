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

/**
 * Exposes authentication endpoints and keeps refresh tokens in HttpOnly cookies.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Registers a new user and starts a session immediately.
     *
     * <p>The refresh token returned by the service is moved to an HttpOnly cookie
     * before the response body is sent, so JavaScript clients never handle it directly.</p>
     *
     * @param request validated registration payload
     * @param servletRequest current HTTP request, used to describe the refresh-token device
     * @param servletResponse current HTTP response, used to append the refresh cookie
     * @return authentication response without the raw refresh token in the JSON body
     */
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

    /**
     * Authenticates an existing user and creates a new refresh-token session.
     *
     * @param request validated login credentials
     * @param servletRequest current HTTP request, used to describe the refresh-token device
     * @param servletResponse current HTTP response, used to append the refresh cookie
     * @return authentication response without the raw refresh token in the JSON body
     */
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

    /**
     * Rotates the current refresh token and issues a new access token.
     *
     * <p>The HttpOnly cookie is the preferred source. The optional body remains as a
     * compatibility fallback for clients that cannot send cookies.</p>
     *
     * @param request optional refresh-token body fallback
     * @param servletRequest current HTTP request, used to read the refresh cookie
     * @param servletResponse current HTTP response, used to replace the refresh cookie
     * @return authentication response with a fresh access token and no refresh token in the body
     */
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

    /**
     * Revokes the current refresh token and clears the browser cookie.
     *
     * @param request optional refresh-token body fallback
     * @param servletRequest current HTTP request, used to read the refresh cookie
     * @param servletResponse current HTTP response, used to expire the refresh cookie
     */
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

    /**
     * Returns the user currently authenticated by the access token.
     *
     * @param user principal resolved by Spring Security
     * @return public user data
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return authService.currentUser(user);
    }

    /**
     * Resolves the refresh token from the safest available source.
     *
     * <p>Cookies win over request bodies because the normal browser flow stores the
     * token as HttpOnly, which reduces accidental token exposure to JavaScript.</p>
     */
    private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        String cookieToken = refreshCookieValue(servletRequest);
        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        return request == null ? null : request.refreshToken();
    }

    /**
     * Finds the refresh-token cookie value if the request carries it.
     */
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

    /**
     * Adds a refresh cookie with the same lifetime as the persisted refresh token.
     */
    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = refreshCookie(refreshToken, Duration.ofMillis(jwtProperties.refreshExpiration()));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Expires the refresh cookie on the client.
     */
    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = refreshCookie("", Duration.ZERO);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Builds the refresh cookie with security attributes configured for the current environment.
     */
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
