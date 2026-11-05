package com.iitsm.repository;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentNumber;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.sla.SlaStatus;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

/**
 * Small, reusable query conditions (JPA Criteria API) that the search combines with
 * {@code Specification.where(a).and(b)...}. Each method returns null when its value is missing, which
 * Spring Data treats as "no condition", so only the filled-in filters apply.
 */
public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    public static Specification<Incident> hasNumber(String number) {
        if (number == null || number.isBlank()) {
            return null;
        }
        long sequence = IncidentNumber.parse(number).sequence();
        return (root, query, cb) -> cb.equal(root.get("sequenceNo"), sequence);
    }

    public static Specification<Incident> textContains(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    public static Specification<Incident> createdFrom(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Incident> createdBefore(Instant to) {
        return to == null ? null : (root, query, cb) -> cb.lessThan(root.get("createdAt"), to);
    }

    public static Specification<Incident> hasStatus(IncidentStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Incident> hasPriority(Priority priority) {
        return priority == null ? null : (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Incident> inCategory(Long categoryId) {
        return categoryId == null ? null : (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Incident> onService(Long serviceId) {
        return serviceId == null ? null : (root, query, cb) -> cb.equal(root.get("service").get("id"), serviceId);
    }

    public static Specification<Incident> assignedToId(Long technicianId) {
        return technicianId == null ? null : (root, query, cb) -> cb.equal(root.get("assignee").get("id"), technicianId);
    }

    public static Specification<Incident> hasSlaStatus(SlaStatus slaStatus) {
        return slaStatus == null ? null : (root, query, cb) -> cb.equal(root.get("slaRecord").get("status"), slaStatus);
    }

    // ----- visibility scopes -----

    public static Specification<Incident> reportedBy(Employee reporter) {
        return (root, query, cb) -> cb.equal(root.get("reporter"), reporter);
    }

    public static Specification<Incident> assignedTo(Technician technician) {
        return (root, query, cb) -> cb.equal(root.get("assignee"), technician);
    }
}
