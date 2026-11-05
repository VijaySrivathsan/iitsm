package com.iitsm.exception;

/**
 * Thrown when assigning would push a technician over their maximum workload.
 */
public class WorkloadExceededException extends AssignmentException {

    public WorkloadExceededException(String technician, int activeIncidents, int maxWorkload) {
        super("Technician " + technician + " already has " + activeIncidents
                + " active incidents (maximum " + maxWorkload + ")");
    }
}
