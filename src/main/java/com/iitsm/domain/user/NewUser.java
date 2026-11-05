package com.iitsm.domain.user;

/**
 * Details common to every new user, passed to {@link UserFactory}. The raw password is hashed by
 * the factory and never stored.
 */
public record NewUser(String username, String rawPassword, String fullName, String email, String phone) {

    @Override
    public String toString() {
        // Never print the password.
        return "NewUser[username=" + username + ", fullName=" + fullName + "]";
    }
}
