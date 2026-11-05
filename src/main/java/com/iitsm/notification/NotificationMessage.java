package com.iitsm.notification;

import com.iitsm.domain.Checks;

/**
 * The content of one notification, independent of how it is delivered.
 *
 * @param urgent true for events that justify interrupting someone (used by the SMS channel)
 */
public record NotificationMessage(String subject, String body, String incidentNumber, boolean urgent) {

    public NotificationMessage {
        Checks.requireText(subject, "Subject");
        Checks.requireText(body, "Body");
    }

    /** Short single-line form for SMS. */
    public String shortText() {
        return "[I-ITSM] " + subject;
    }
}
