package com.iitsm.domain.catalog;

import com.iitsm.domain.BaseEntity;
import com.iitsm.domain.Checks;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * An IT service the organisation runs (Payment Gateway, Email, VPN, ...). Its business criticality
 * (1 = minor, 5 = business-critical) is one of the inputs to incident priority.
 */
@Entity
@Table(name = "it_service")
public class ITService extends BaseEntity {

    public static final int MIN_CRITICALITY = 1;
    public static final int MAX_CRITICALITY = 5;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    private int businessCriticality;

    private boolean active = true;

    protected ITService() {
    }

    public ITService(String name, String description, int businessCriticality) {
        this.name = Checks.requireText(name, "Service name");
        this.description = Checks.optionalText(description);
        this.businessCriticality = Checks.requireRange(businessCriticality, MIN_CRITICALITY, MAX_CRITICALITY,
                "Business criticality");
    }

    public void update(String description, int businessCriticality) {
        this.description = Checks.optionalText(description);
        this.businessCriticality = Checks.requireRange(businessCriticality, MIN_CRITICALITY, MAX_CRITICALITY,
                "Business criticality");
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getBusinessCriticality() {
        return businessCriticality;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return name;
    }
}
