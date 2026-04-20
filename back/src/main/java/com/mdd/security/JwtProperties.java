package com.mdd.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        long expiration,
        long refreshExpiration,
        String refreshCookieName,
        boolean refreshCookieSecure,
        String refreshCookieSameSite
) {
}
