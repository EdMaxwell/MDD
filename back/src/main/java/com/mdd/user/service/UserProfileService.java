package com.mdd.user.service;

import com.mdd.auth.domain.User;
import com.mdd.auth.repository.UserRepository;
import com.mdd.auth.exception.EmailAlreadyUsedException;
import com.mdd.topic.domain.Topic;
import com.mdd.user.dto.SubscriptionResponse;
import com.mdd.user.dto.UpdateProfileRequest;
import com.mdd.user.dto.UserProfileResponse;
import com.mdd.user.exception.UserNotFoundException;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and updates the authenticated user's profile and subscriptions.
 */
@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Returns the authenticated user's profile with subscriptions sorted for display.
     *
     * @param authenticatedUser principal resolved by Spring Security
     * @return profile response
     */
    @Transactional(readOnly = true)
    public UserProfileResponse currentProfile(User authenticatedUser) {
        return toProfileResponse(findUserWithSubscriptions(authenticatedUser));
    }

    /**
     * Updates profile fields and optionally replaces the password.
     *
     * <p>An empty password means "keep the current password", which matches the profile
     * form behavior and avoids accidentally hashing an empty value.</p>
     *
     * @param authenticatedUser principal resolved by Spring Security
     * @param request validated profile update payload
     * @return updated profile response
     */
    @Transactional
    public UserProfileResponse updateProfile(User authenticatedUser, UpdateProfileRequest request) {
        User user = findUserWithSubscriptions(authenticatedUser);

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, user.getId())) {
            throw new EmailAlreadyUsedException(normalizedEmail);
        }

        user.updateProfile(request.name().trim(), normalizedEmail);

        String rawPassword = request.password() == null ? "" : request.password().trim();
        if (!rawPassword.isEmpty()) {
            user.updatePassword(passwordEncoder.encode(rawPassword));
        }

        return toProfileResponse(userRepository.save(user));
    }

    /**
     * Reloads the principal with subscriptions to build a complete profile response.
     */
    private User findUserWithSubscriptions(User authenticatedUser) {
        return userRepository.findWithSubscriptionsById(authenticatedUser.getId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.getId()));
    }

    /**
     * Maps the user entity to the profile API contract and sorts subscriptions by topic name.
     */
    private UserProfileResponse toProfileResponse(User user) {
        List<SubscriptionResponse> subscriptions = user.getSubscriptions().stream()
                .sorted(Comparator.comparing(Topic::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toSubscriptionResponse)
                .toList();

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                subscriptions
        );
    }

    /**
     * Maps a subscribed topic to the profile subscription API contract.
     */
    private SubscriptionResponse toSubscriptionResponse(Topic topic) {
        return new SubscriptionResponse(topic.getId(), topic.getName(), topic.getDescription());
    }
}
