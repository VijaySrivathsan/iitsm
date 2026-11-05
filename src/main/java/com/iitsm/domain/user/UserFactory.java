package com.iitsm.domain.user;

import com.iitsm.domain.Checks;
import com.iitsm.exception.InvalidInputException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Factory pattern: the single place that creates {@link User} objects. It validates the input,
 * hashes the password with BCrypt and returns the right subtype for the requested {@link Role}.
 * Callers never call the subclass constructors directly, so password handling cannot be skipped.
 */
@Component
public class UserFactory {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private final PasswordEncoder passwordEncoder;

    public UserFactory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates a user of the given role with sensible defaults for role-specific fields. */
    public User create(Role role, NewUser details) {
        Checks.requireNonNull(role, "Role");
        return switch (role) {
            case EMPLOYEE -> createEmployee(details, null);
            case TECHNICIAN -> createTechnician(details, Technician.DEFAULT_MAX_WORKLOAD, 1);
            case ADMIN -> createAdministrator(details, false);
        };
    }

    public Employee createEmployee(NewUser details, String department) {
        return new Employee(username(details), hash(details), details.fullName(), details.email(),
                details.phone(), department);
    }

    public Technician createTechnician(NewUser details, int maxWorkload, int experienceLevel) {
        return new Technician(username(details), hash(details), details.fullName(), details.email(),
                details.phone(), maxWorkload, experienceLevel);
    }

    public Administrator createAdministrator(NewUser details, boolean itManager) {
        return new Administrator(username(details), hash(details), details.fullName(), details.email(),
                details.phone(), itManager);
    }

    /** Hashes a new password for an existing user (password change / reset). */
    public String hashPassword(String rawPassword) {
        validatePassword(rawPassword);
        return passwordEncoder.encode(rawPassword);
    }

    private String username(NewUser details) {
        Checks.requireNonNull(details, "User details");
        String username = Checks.requireText(details.username(), "Username");
        if (!username.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new InvalidInputException(
                    "Username must be 3-50 characters: letters, digits, dot, underscore or dash");
        }
        return username;
    }

    private String hash(NewUser details) {
        return hashPassword(details.rawPassword());
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidInputException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }
}
