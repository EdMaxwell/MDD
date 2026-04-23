package com.mdd.auth.service;

import com.mdd.auth.domain.User;
import com.mdd.auth.dto.AuthResponse;
import com.mdd.auth.dto.LoginRequest;
import com.mdd.auth.dto.RefreshTokenRequest;
import com.mdd.auth.dto.RegisterRequest;
import com.mdd.auth.dto.UserResponse;
import com.mdd.auth.exception.EmailAlreadyUsedException;
import com.mdd.auth.exception.InvalidCredentialsException;
import com.mdd.auth.exception.InvalidRefreshTokenException;
import com.mdd.auth.repository.UserRepository;
import com.mdd.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates registration, login, token refresh and current-user mapping.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Creates a user account, normalizes the email and immediately returns session tokens.
     *
     * @param request registration data validated by the controller
     * @param servletRequest current request, forwarded to refresh-token creation for device metadata
     * @return access token, raw refresh token and public user data
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest servletRequest) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyUsedException(normalizedEmail);
        }

        User user = new User(
                request.name().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password())
        );
        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser, servletRequest);
    }

    /**
     * Authenticates the submitted credentials through Spring Security.
     *
     * @param request login credentials validated by the controller; the email field accepts email or username
     * @param servletRequest current request, forwarded to refresh-token creation for device metadata
     * @return access token, raw refresh token and public user data
     */
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        try {
            String loginIdentifier = request.email().trim().toLowerCase();
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginIdentifier, request.password())
            );
            User user = (User) authentication.getPrincipal();
            return buildAuthResponse(user, servletRequest);
        } catch (BadCredentialsException exception) {
            throw new InvalidCredentialsException();
        }
    }

    /**
     * Rotates a refresh token coming from the legacy request body contract.
     *
     * @param request refresh-token payload
     * @param servletRequest current request, forwarded to refresh-token creation for device metadata
     * @return a fresh access token and replacement refresh token
     */
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        return refresh(request.refreshToken(), servletRequest);
    }

    /**
     * Rotates a raw refresh token and issues a new access token for the same user.
     *
     * @param rawRefreshToken raw refresh token from the cookie or compatibility body
     * @param servletRequest current request, forwarded to refresh-token creation for device metadata
     * @return a fresh access token and replacement refresh token
     */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken, HttpServletRequest servletRequest) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshTokenRotation rotation = refreshTokenService.rotate(rawRefreshToken, servletRequest);
        String accessToken = jwtService.generateToken(rotation.user());
        return new AuthResponse(accessToken, rotation.refreshToken(), toUserResponse(rotation.user()));
    }

    /**
     * Revokes a refresh token coming from the legacy request body contract.
     *
     * @param request refresh-token payload
     */
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    /**
     * Revokes a raw refresh token when it is known and ignores invalid tokens for logout.
     *
     * <p>Logout is intentionally idempotent: the client must be able to clear its
     * local cookie even if the server-side token was already removed or expired.</p>
     *
     * @param rawRefreshToken raw refresh token from the cookie or compatibility body
     */
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        try {
            refreshTokenService.revoke(rawRefreshToken);
        } catch (InvalidRefreshTokenException exception) {
            // Logout remains idempotent so the client can always clear its HttpOnly cookie.
        }
    }

    /**
     * Maps the authenticated principal to the public user DTO.
     *
     * @param user authenticated principal
     * @return public user data
     */
    public UserResponse currentUser(User user) {
        return toUserResponse(user);
    }

    /**
     * Generates both access and refresh tokens for an authenticated user.
     */
    private AuthResponse buildAuthResponse(User user, HttpServletRequest request) {
        String token = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createFor(user, request);
        return new AuthResponse(token, refreshToken, toUserResponse(user));
    }

    /**
     * Keeps the API response independent from the JPA entity and Spring Security principal.
     */
    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
