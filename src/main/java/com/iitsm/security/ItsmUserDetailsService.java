package com.iitsm.security;

import com.iitsm.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tells Spring Security how to find a user at login. Password checking (BCrypt) and the "invalid
 * login" response are handled by Spring Security itself.
 */
@Service
public class ItsmUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public ItsmUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsernameIgnoreCase(username)
                .map(ItsmUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
