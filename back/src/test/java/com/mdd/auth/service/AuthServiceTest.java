package com.mdd.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mdd.auth.domain.User;
import com.mdd.auth.dto.LoginRequest;
import com.mdd.auth.dto.RefreshTokenRequest;
import com.mdd.auth.dto.RegisterRequest;
import com.mdd.auth.exception.EmailAlreadyUsedException;
import com.mdd.auth.exception.InvalidCredentialsException;
import com.mdd.auth.repository.UserRepository;
import com.mdd.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for authentication registration, login and refresh workflows.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService, refreshTokenService);
    }

    @Test
    void registerShouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest("Alice", "Alice@Example.com", "Password1!");
        User savedUser = new User("Alice", "alice@example.com", "hashed-password");

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token");
        when(refreshTokenService.createFor(savedUser, null)).thenReturn("refresh-token");

        var response = authService.register(request, null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(userCaptor.getValue().getName()).isEqualTo("Alice");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("alice@example.com");
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "Password1!");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, null))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void loginShouldReturnTokenForValidCredentials() {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        LoginRequest request = new LoginRequest("Alice", "password123");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(refreshTokenService.createFor(user, null)).thenReturn("refresh-token");

        var response = authService.login(request, null);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());

        assertThat(authenticationCaptor.getValue().getPrincipal()).isEqualTo("alice");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("alice@example.com");
    }

    @Test
    void refreshShouldRotateRefreshTokenAndReturnNewAccessToken() {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");

        when(refreshTokenService.rotate("old-refresh-token", null))
                .thenReturn(new RefreshTokenRotation(user, "new-refresh-token"));
        when(jwtService.generateToken(user)).thenReturn("new-jwt-token");

        var response = authService.refresh(request, null);

        assertThat(response.token()).isEqualTo("new-jwt-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.user().email()).isEqualTo("alice@example.com");
    }

    @Test
    void loginShouldRejectInvalidCredentials() {
        LoginRequest request = new LoginRequest("alice@example.com", "wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request, null))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
