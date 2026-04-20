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

    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
            );
            User user = (User) authentication.getPrincipal();
            return buildAuthResponse(user, servletRequest);
        } catch (BadCredentialsException exception) {
            throw new InvalidCredentialsException();
        }
    }

    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        return refresh(request.refreshToken(), servletRequest);
    }

    public AuthResponse refresh(String rawRefreshToken, HttpServletRequest servletRequest) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshTokenRotation rotation = refreshTokenService.rotate(rawRefreshToken, servletRequest);
        String accessToken = jwtService.generateToken(rotation.user());
        return new AuthResponse(accessToken, rotation.refreshToken(), toUserResponse(rotation.user()));
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

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

    public UserResponse currentUser(User user) {
        return toUserResponse(user);
    }

    private AuthResponse buildAuthResponse(User user, HttpServletRequest request) {
        String token = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createFor(user, request);
        return new AuthResponse(token, refreshToken, toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
