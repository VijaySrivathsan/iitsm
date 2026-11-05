package com.iitsm.assignment;

import com.iitsm.domain.user.Technician;

/**
 * A row of the technician workload report.
 */
public record TechnicianWorkload(Technician technician, long activeIncidents) {

    public int maxWorkload() {
        return technician.getMaxWorkload();
    }

    public boolean isFull() {
        return activeIncidents >= technician.getMaxWorkload();
    }

    /** Percentage of capacity in use (0-100+), for progress bars. */
    public int percentUsed() {
        return (int) Math.round(100.0 * activeIncidents / technician.getMaxWorkload());
    }
}
