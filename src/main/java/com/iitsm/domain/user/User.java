package com.iitsm.domain.user;

import com.iitsm.domain.BaseEntity;
import com.iitsm.domain.Checks;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Anyone who can log in. Abstract because every real user is exactly one of
 * {@link Employee}, {@link Technician} or {@link Administrator} (genuine "is-a" relationships).
 * Stored in one table (SINGLE_TABLE) - see docs/decisions.md D4.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public abstract class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 120)
    private String email;

    @Column(length = 20)
    private String phone;

    private boolean active = true;

    /** For JPA only. */
    protected User() {
    }

    protected User(String username, String passwordHash, String fullName, String email, String phone) {
        this.username = Checks.requireText(username, "Username").toLowerCase();
        this.passwordHash = Checks.requireText(passwordHash, "Password hash");
        this.fullName = Checks.requireText(fullName, "Full name");
        this.email = Checks.optionalText(email);
        this.phone = Checks.optionalText(phone);
    }

    /** The security role this kind of user has. Each subclass answers differently (polymorphism). */
    public abstract Role getRole();

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = Checks.requireText(newPasswordHash, "Password hash");
    }

    public void updateContactDetails(String fullName, String email, String phone) {
        this.fullName = Checks.requireText(fullName, "Full name");
        this.email = Checks.optionalText(email);
        this.phone = Checks.optionalText(phone);
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return getRole().label() + " " + username;
    }
}
