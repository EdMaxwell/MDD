package com.mdd.auth.service;

import com.mdd.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads MDD users for Spring Security using either email or username as login identifier.
 */
@Service
public class MddUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public MddUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by email or username for Spring Security authentication and JWT validation.
     *
     * @param username email, username or JWT subject submitted as the Spring Security username
     * @return user details used as the authenticated principal
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedIdentifier = username.trim().toLowerCase();
        return userRepository.findByEmailIgnoreCaseOrNameIgnoreCase(normalizedIdentifier, normalizedIdentifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
