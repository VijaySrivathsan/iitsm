package com.iitsm.domain.incident;

/**
 * Kind of entry in an incident's conversation/investigation log.
 */
public enum NoteType {
    /** Technician's internal investigation note. */
    INVESTIGATION,
    /** Technician asks the reporter for more information. */
    INFO_REQUEST,
    /** Reporter answers an information request or adds details. */
    INFO_RESPONSE
}
