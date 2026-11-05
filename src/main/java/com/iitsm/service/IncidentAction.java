package com.iitsm.service;

/**
 * Actions a particular user may take on a particular incident right now. Combines the lifecycle
 * rules (State pattern) with the role/ownership rules, so the UI only shows buttons that will work.
 */
public enum IncidentAction {
    CLASSIFY,
    ASSIGN,
    ACKNOWLEDGE,
    START_WORK,
    REQUEST_INFO,
    PROVIDE_INFO,
    ADD_NOTE,
    RESOLVE,
    VERIFY,
    REOPEN,
    CLOSE,
    CHANGE_IMPACT,
    REVIEW_DUPLICATE
}
