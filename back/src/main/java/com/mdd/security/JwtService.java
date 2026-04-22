package com.mdd.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Creates and validates signed JWT access tokens.
 */
@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final Clock clock;

    public JwtService(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    /**
     * Generates a signed access token whose subject is the Spring Security username.
     *
     * @param userDetails authenticated user details
     * @return compact JWT access token
     */
    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now(clock);
        Instant expiration = now.plusMillis(jwtProperties.expiration());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the token subject without changing authentication state.
     *
     * @param token compact JWT access token
     * @return username stored as the JWT subject
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Verifies that the token belongs to the given user and is not expired.
     *
     * @param token compact JWT access token
     * @param userDetails user details loaded from the database
     * @return true when the token subject matches the user and the token is still valid
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Checks expiration against the injected clock so tests can control time.
     */
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().toInstant().isBefore(Instant.now(clock));
    }

    /**
     * Parses and validates all JWT claims using the configured signing key.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Decodes the Base64 HMAC secret configured for the application.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
