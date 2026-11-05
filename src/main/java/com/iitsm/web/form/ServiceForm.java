package com.iitsm.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Administrator's form for an IT service.
 */
public class ServiceForm {

    @NotBlank
    @Size(max = 80)
    private String name;

    @Size(max = 255)
    private String description;

    @Min(1)
    @Max(5)
    private int businessCriticality = 3;

    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getBusinessCriticality() {
        return businessCriticality;
    }

    public void setBusinessCriticality(int businessCriticality) {
        this.businessCriticality = businessCriticality;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
