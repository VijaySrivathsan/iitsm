package com.iitsm.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * An administrator / IT manager who configures the system and monitors incidents.
 * The {@code itManager} flag marks the top level of the escalation chain.
 */
@Entity
@DiscriminatorValue("ADMIN")
public class Administrator extends User {

    private Boolean itManager;

    protected Administrator() {
    }

    public Administrator(String username, String passwordHash, String fullName, String email, String phone,
                         boolean itManager) {
        super(username, passwordHash, fullName, email, phone);
        this.itManager = itManager;
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    public boolean isItManager() {
        return Boolean.TRUE.equals(itManager);
    }

    public void makeItManager() {
        this.itManager = true;
    }
}
