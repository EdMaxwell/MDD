package com.mdd.auth.service;

import com.mdd.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads MDD users for Spring Security using their email address as username.
 */
@Service
public class MddUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public MddUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by email for Spring Security authentication and JWT validation.
     *
     * @param username email submitted as the Spring Security username
     * @return user details used as the authenticated principal
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
