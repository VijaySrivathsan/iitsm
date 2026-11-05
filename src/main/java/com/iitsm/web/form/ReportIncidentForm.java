package com.iitsm.web.form;

import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Urgency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The employee's "report an incident" form.
 */
public class ReportIncidentForm {

    @NotNull(message = "choose the affected service")
    private Long serviceId;

    private Long categoryId;

    @Size(max = 150)
    private String title;

    @NotBlank(message = "describe the problem")
    @Size(max = 4000)
    private String description;

    @NotNull
    private Impact impact = Impact.MEDIUM;

    @NotNull
    private Urgency urgency = Urgency.MEDIUM;

    @NotNull
    @Min(1)
    @Max(1000000)
    private Integer affectedUsers = 1;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Impact getImpact() {
        return impact;
    }

    public void setImpact(Impact impact) {
        this.impact = impact;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public Integer getAffectedUsers() {
        return affectedUsers;
    }

    public void setAffectedUsers(Integer affectedUsers) {
        this.affectedUsers = affectedUsers;
    }
}
