package com.iitsm.notification;

/**
 * Groups of people who may need to hear about an incident event.
 */
public enum Audience {
    REPORTER,
    ASSIGNEE,
    TEAM_LEADS,
    ADMINS,
    /** The person an incident was just escalated to. */
    ESCALATION_TARGET
}
