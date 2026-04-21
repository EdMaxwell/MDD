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

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse currentProfile(User authenticatedUser) {
        User user = userRepository.findWithSubscriptionsById(authenticatedUser.getId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.getId()));

        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(User authenticatedUser, UpdateProfileRequest request) {
        User user = userRepository.findWithSubscriptionsById(authenticatedUser.getId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.getId()));

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

    private SubscriptionResponse toSubscriptionResponse(Topic topic) {
        return new SubscriptionResponse(topic.getId(), topic.getName(), topic.getDescription());
    }
}
