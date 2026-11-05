package com.iitsm.security;

import com.iitsm.domain.user.User;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Gives controllers the logged-in {@link User} entity (freshly loaded, so role-specific fields such
 * as a technician's skills are up to date).
 */
@Component
public class CurrentUserProvider {

    private final UserRepository users;

    public CurrentUserProvider(UserRepository users) {
        this.users = users;
    }

    public Optional<User> currentUserIfLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ItsmUserPrincipal principal)) {
            return Optional.empty();
        }
        return users.findById(principal.getUserId());
    }

    public User currentUser() {
        return currentUserIfLoggedIn()
                .orElseThrow(() -> new UnauthorizedOperationException("Please log in"));
    }
}
