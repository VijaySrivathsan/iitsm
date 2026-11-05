package com.iitsm.service;

import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.sla.SlaStatus;

import java.time.LocalDate;

/**
 * The search form: every field is optional, and the filters that are filled in are combined with AND.
 * Nine criteria (the brief asks for at least five).
 *
 * @param number   incident number, e.g. "INC1001" (exact)
 * @param text     words to find in the title or description
 * @param from     created on or after this day
 * @param to       created on or before this day
 */
public record IncidentSearchCriteria(String number,
                                     String text,
                                     LocalDate from,
                                     LocalDate to,
                                     IncidentStatus status,
                                     Priority priority,
                                     Long categoryId,
                                     Long serviceId,
                                     Long technicianId,
                                     SlaStatus slaStatus) {

    public static IncidentSearchCriteria none() {
        return new IncidentSearchCriteria(null, null, null, null, null, null, null, null, null, null);
    }

    public boolean isEmpty() {
        return blank(number) && blank(text) && from == null && to == null && status == null && priority == null
                && categoryId == null && serviceId == null && technicianId == null && slaStatus == null;
    }

    static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
