package com.iitsm.web.form;

import com.iitsm.domain.incident.Priority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Administrator's form for an SLA policy (no service = default policy).
 */
public class SlaPolicyForm {

    private Long serviceId;

    @NotNull
    private Priority priority;

    @Min(1)
    @Max(100000)
    private int responseMinutes = 60;

    @Min(1)
    @Max(100000)
    private int resolutionMinutes = 480;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public int getResponseMinutes() {
        return responseMinutes;
    }

    public void setResponseMinutes(int responseMinutes) {
        this.responseMinutes = responseMinutes;
    }

    public int getResolutionMinutes() {
        return resolutionMinutes;
    }

    public void setResolutionMinutes(int resolutionMinutes) {
        this.resolutionMinutes = resolutionMinutes;
    }
}
