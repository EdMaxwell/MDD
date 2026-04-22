package com.mdd.user.controller;

import com.mdd.auth.domain.User;
import com.mdd.user.dto.UpdateProfileRequest;
import com.mdd.user.dto.UserProfileResponse;
import com.mdd.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes profile read and update endpoints for the authenticated user.
 */
@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * Returns the authenticated user's profile.
     *
     * @param user principal resolved from the access token
     * @return profile including followed topics
     */
    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal User user) {
        return userProfileService.currentProfile(user);
    }

    /**
     * Updates the authenticated user's profile.
     *
     * @param user principal resolved from the access token
     * @param request validated profile update payload
     * @return updated profile
     */
    @PutMapping("/me")
    public UserProfileResponse update(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userProfileService.updateProfile(user, request);
    }
}
