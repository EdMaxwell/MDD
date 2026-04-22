package com.mdd.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mdd.auth.domain.User;
import com.mdd.auth.exception.EmailAlreadyUsedException;
import com.mdd.auth.repository.UserRepository;
import com.mdd.topic.domain.Topic;
import com.mdd.user.dto.UpdateProfileRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for profile reading and profile updates.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userRepository, passwordEncoder);
    }

    @Test
    void currentProfileShouldReturnSortedSubscriptions() {
        User authenticatedUser = new User("Alice", "alice@example.com", "hashed-password");
        Topic spring = new Topic("Spring", "Framework Java");
        Topic angular = new Topic("Angular", "Framework frontend");
        ReflectionTestUtils.setField(authenticatedUser, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        ReflectionTestUtils.setField(spring, "id", UUID.fromString("00000000-0000-0000-0000-000000000011"));
        ReflectionTestUtils.setField(angular, "id", UUID.fromString("00000000-0000-0000-0000-000000000012"));

        authenticatedUser.getSubscriptions().add(spring);
        authenticatedUser.getSubscriptions().add(angular);

        when(userRepository.findWithSubscriptionsById(authenticatedUser.getId())).thenReturn(Optional.of(authenticatedUser));

        var profile = userProfileService.currentProfile(authenticatedUser);

        assertThat(profile.email()).isEqualTo("alice@example.com");
        assertThat(profile.subscriptions()).hasSize(2);
        assertThat(profile.subscriptions().getFirst().name()).isEqualTo("Angular");
        assertThat(profile.subscriptions().get(1).name()).isEqualTo("Spring");
    }

    @Test
    void updateProfileShouldNormalizeEmailAndEncodePassword() {
        User authenticatedUser = new User("Alice", "alice@example.com", "hashed-password");
        UpdateProfileRequest request = new UpdateProfileRequest("Alice Cooper", "Alice.New@Example.com", "NewPassword1!");
        ReflectionTestUtils.setField(authenticatedUser, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));

        when(userRepository.findWithSubscriptionsById(authenticatedUser.getId())).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("alice.new@example.com", authenticatedUser.getId())).thenReturn(false);
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userProfileService.updateProfile(authenticatedUser, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getName()).isEqualTo("Alice Cooper");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("alice.new@example.com");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(response.email()).isEqualTo("alice.new@example.com");
    }

    @Test
    void updateProfileShouldKeepPasswordWhenBlank() {
        User authenticatedUser = new User("Alice", "alice@example.com", "hashed-password");
        UpdateProfileRequest request = new UpdateProfileRequest("Alice Cooper", "alice@example.com", "");
        ReflectionTestUtils.setField(authenticatedUser, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));

        when(userRepository.findWithSubscriptionsById(authenticatedUser.getId())).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("alice@example.com", authenticatedUser.getId())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userProfileService.updateProfile(authenticatedUser, request);

        assertThat(authenticatedUser.getPassword()).isEqualTo("hashed-password");
        assertThat(response.name()).isEqualTo("Alice Cooper");
    }

    @Test
    void updateProfileShouldRejectDuplicateEmail() {
        User authenticatedUser = new User("Alice", "alice@example.com", "hashed-password");
        UpdateProfileRequest request = new UpdateProfileRequest("Alice", "taken@example.com", "NewPassword1!");
        ReflectionTestUtils.setField(authenticatedUser, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));

        when(userRepository.findWithSubscriptionsById(authenticatedUser.getId())).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", authenticatedUser.getId())).thenReturn(true);

        assertThatThrownBy(() -> userProfileService.updateProfile(authenticatedUser, request))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }
}
