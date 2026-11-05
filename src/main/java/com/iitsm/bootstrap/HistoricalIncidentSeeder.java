package com.iitsm.bootstrap;

import com.iitsm.domain.audit.AuditEntry;
import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Feedback;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentNumber;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.NoteType;
import com.iitsm.domain.incident.Resolution;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.sla.SlaPolicy;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.priority.PriorityInput;
import com.iitsm.priority.PriorityResult;
import com.iitsm.priority.PriorityService;
import com.iitsm.repository.AuditEntryRepository;
import com.iitsm.repository.CategoryRepository;
import com.iitsm.repository.EmployeeRepository;
import com.iitsm.repository.ITServiceRepository;
import com.iitsm.repository.IncidentRepository;
import com.iitsm.repository.TechnicianRepository;
import com.iitsm.sla.SlaService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Creates about 30 past incidents (2-20 days old) so dashboards, search, reports and the
 * "suggested resolution" feature have realistic data. It drives the real domain model (priority
 * strategy, SLA policy, State pattern transitions) with past timestamps, and writes matching audit
 * lines. Called by DemoDataLoader on an empty database only.
 *
 * <p>Nothing here is recent and almost nothing is open, so live demo reports are not flagged as
 * duplicates of history and tech1 starts the demo with an empty queue.
 */
@Component
public class HistoricalIncidentSeeder {

    /** How an old incident ended. */
    enum Outcome { CLOSED_ON_TIME, CLOSED_LATE, RESOLVED_AWAITING_VERIFY, OPEN_OVERDUE }

    /** One past incident: who/what/how it ended. */
    record Past(int daysAgo, String reporter, String service, String category, String title, String description,
                Impact impact, Urgency urgency, int users, String technician, Outcome outcome,
                String fix, String rootCause, int rating) {
    }

    private static final List<Past> HISTORY = List.of(
            past(19, "emp1", "Email", "SOFTWARE", "Outlook keeps asking for password", "Outlook prompts for the password every few minutes", Impact.LOW, Urgency.MEDIUM, 1, "lead1", Outcome.CLOSED_ON_TIME, "Cleared cached credentials and re-added the account", "Stale token after password change", 5),
            past(18, "emp2", "VPN", "NETWORK", "VPN disconnects every 10 minutes", "VPN drops connection frequently when working from home", Impact.MEDIUM, Urgency.MEDIUM, 12, "tech2", Outcome.CLOSED_ON_TIME, "Raised VPN idle timeout and pushed new client profile", "Idle timeout set too low after upgrade", 4),
            past(18, "emp3", "Core Database", "DATABASE", "Reports query timing out", "Monthly sales report query times out after 30 seconds", Impact.MEDIUM, Urgency.HIGH, 25, "tech1", Outcome.CLOSED_LATE, "Added index on orders.created_at and rewrote report query", "Missing index after schema change", 3),
            past(17, "emp1", "Payment Gateway", "APPLICATION", "Card payments failing at checkout", "Customers see an error when paying by card at checkout", Impact.HIGH, Urgency.HIGH, 800, "tech1", Outcome.CLOSED_ON_TIME, "Rolled back payment service to release 4.1", "Bad configuration in release 4.2", 5),
            past(16, "emp2", "Auth / SSO", "AUTHENTICATION", "Cannot log in with SSO", "Single sign-on login page shows an error for sales team", Impact.HIGH, Urgency.HIGH, 60, "tech3", Outcome.CLOSED_ON_TIME, "Renewed expired SAML signing certificate", "Certificate expired", 5),
            past(15, "emp3", "Cloud Storage", "STORAGE", "Shared drive full", "Team shared drive shows quota exceeded", Impact.MEDIUM, Urgency.MEDIUM, 15, "lead1", Outcome.CLOSED_ON_TIME, "Increased team quota and archived old project folders", "Quota not reviewed for a year", 4),
            past(15, "emp1", "HR Portal", "APPLICATION", "Leave balance shows wrong value", "HR portal leave balance is two days less than expected", Impact.LOW, Urgency.LOW, 1, "tech1", Outcome.CLOSED_ON_TIME, "Re-ran leave accrual job for October", "Accrual job skipped one run", 4),
            past(14, "emp2", "Email", "SOFTWARE", "Emails stuck in outbox", "Messages with attachments stay in the outbox", Impact.LOW, Urgency.MEDIUM, 3, "lead1", Outcome.CLOSED_ON_TIME, "Raised attachment size limit on mail gateway", "Gateway limit lower than client limit", 4),
            past(13, "emp3", "VPN", "NETWORK", "VPN very slow", "VPN connection is very slow in the afternoon", Impact.MEDIUM, Urgency.LOW, 30, "tech2", Outcome.CLOSED_LATE, "Added second VPN concentrator and load balancing", "Single concentrator saturated at peak", 3),
            past(12, "emp1", "Payment Gateway", "APPLICATION", "UPI payments timing out", "UPI transactions time out after 60 seconds", Impact.HIGH, Urgency.HIGH, 400, "tech1", Outcome.CLOSED_ON_TIME, "Increased connection pool to the UPI provider", "Connection pool exhausted under load", 5),
            past(12, "emp2", "Core Database", "DATABASE", "Database deadlocks on orders table", "Order updates fail with deadlock errors", Impact.HIGH, Urgency.MEDIUM, 40, "tech1", Outcome.CLOSED_ON_TIME, "Changed update order in batch job to avoid lock cycles", "Batch job and API locked rows in opposite order", 4),
            past(11, "emp3", "Auth / SSO", "AUTHENTICATION", "Account locked after password reset", "User account locked right after resetting password", Impact.LOW, Urgency.HIGH, 1, "tech3", Outcome.CLOSED_ON_TIME, "Unlocked account and cleared old sessions", "Old sessions retried with the old password", 5),
            past(10, "emp1", "Cloud Storage", "STORAGE", "Backup job failed", "Nightly backup of shared drives did not finish", Impact.MEDIUM, Urgency.MEDIUM, 1, "lead1", Outcome.CLOSED_ON_TIME, "Freed space on backup target and re-ran job", "Backup target disk full", 4),
            past(10, "emp2", "Email", "SECURITY", "Phishing email received", "Suspicious email asking to reset password via a link", Impact.MEDIUM, Urgency.HIGH, 50, "tech3", Outcome.CLOSED_ON_TIME, "Blocked sender domain and purged message from all mailboxes", "Phishing campaign", 5),
            past(9, "emp3", "HR Portal", "APPLICATION", "Payslip download error", "Downloading payslip PDF shows an error", Impact.LOW, Urgency.MEDIUM, 10, "tech1", Outcome.CLOSED_ON_TIME, "Fixed PDF template font path", "Font missing after server move", 4),
            past(8, "emp1", "VPN", "NETWORK", "Cannot connect to VPN from hotel wifi", "VPN fails to connect on hotel networks", Impact.LOW, Urgency.MEDIUM, 2, "tech2", Outcome.CLOSED_ON_TIME, "Enabled VPN over TCP 443 fallback", "Hotel network blocks UDP", 5),
            past(8, "emp2", "Payment Gateway", "APPLICATION", "Refunds not processed", "Refunds requested yesterday are still pending", Impact.MEDIUM, Urgency.MEDIUM, 20, "tech1", Outcome.CLOSED_LATE, "Restarted refund worker and replayed queue", "Refund worker crashed silently", 2),
            past(7, "emp3", "Core Database", "DATABASE", "Replica lag growing", "Read replica is hours behind primary", Impact.MEDIUM, Urgency.HIGH, 10, "tech1", Outcome.CLOSED_ON_TIME, "Tuned replication and moved reporting to separate replica", "Heavy reporting queries on the replica", 4),
            past(6, "emp1", "Auth / SSO", "AUTHENTICATION", "OTP not received", "Login OTP SMS not arriving for some users", Impact.MEDIUM, Urgency.HIGH, 35, "tech3", Outcome.CLOSED_ON_TIME, "Switched OTP to backup SMS provider", "Primary SMS provider outage", 4),
            past(5, "emp2", "Cloud Storage", "STORAGE", "File sync conflicts", "Desktop sync creates duplicate conflicted files", Impact.LOW, Urgency.LOW, 4, "lead1", Outcome.CLOSED_ON_TIME, "Updated sync client to latest version", "Known bug in old client", 4),
            past(5, "emp3", "Email", "SOFTWARE", "Calendar invites not updating", "Meeting updates do not appear in calendars", Impact.LOW, Urgency.MEDIUM, 8, "lead1", Outcome.CLOSED_ON_TIME, "Rebuilt calendar search index", "Corrupted index", 4),
            past(4, "emp1", "Payment Gateway", "APPLICATION", "Payment page loading slowly", "Checkout payment page takes 20 seconds to load", Impact.MEDIUM, Urgency.HIGH, 300, "tech1", Outcome.CLOSED_ON_TIME, "Enabled CDN caching for payment page assets", "Static assets served from origin", 5),
            past(4, "emp2", "VPN", "NETWORK", "VPN certificate warning", "VPN client shows certificate not trusted", Impact.MEDIUM, Urgency.MEDIUM, 40, "tech2", Outcome.CLOSED_ON_TIME, "Installed new intermediate certificate on gateway", "Incomplete certificate chain", 4),
            past(3, "emp3", "HR Portal", "APPLICATION", "Cannot submit expense claim", "Submit button on expense form does nothing", Impact.LOW, Urgency.MEDIUM, 5, "tech1", Outcome.RESOLVED_AWAITING_VERIFY, "Fixed JavaScript error on expense form", "Browser update broke date picker", 0),
            past(3, "emp1", "Core Database", "DATABASE", "Slow search in CRM", "Customer search in CRM takes over a minute", Impact.MEDIUM, Urgency.MEDIUM, 25, "lead1", Outcome.RESOLVED_AWAITING_VERIFY, "Rebuilt fragmented indexes", "Index fragmentation", 0),
            past(2, "emp2", "Cloud Storage", "STORAGE", "Cannot share files externally", "External sharing links are disabled", Impact.LOW, Urgency.MEDIUM, 6, "lead1", Outcome.OPEN_OVERDUE, null, null, 0),
            past(2, "emp3", "Auth / SSO", "SECURITY", "Suspicious login alerts", "Several users received alerts about logins from abroad", Impact.HIGH, Urgency.MEDIUM, 12, "tech3", Outcome.OPEN_OVERDUE, null, null, 0),
            past(2, "emp1", "VPN", "NETWORK", "VPN split tunnelling not working", "Internal sites unreachable while on VPN", Impact.MEDIUM, Urgency.MEDIUM, 9, "tech2", Outcome.OPEN_OVERDUE, null, null, 0));

    private static Past past(int daysAgo, String reporter, String service, String category, String title,
                             String description, Impact impact, Urgency urgency, int users, String technician,
                             Outcome outcome, String fix, String rootCause, int rating) {
        return new Past(daysAgo, reporter, service, category, title, description, impact, urgency, users, technician,
                outcome, fix, rootCause, rating);
    }

    private final IncidentRepository incidents;
    private final EmployeeRepository employees;
    private final TechnicianRepository technicians;
    private final ITServiceRepository services;
    private final CategoryRepository categories;
    private final AuditEntryRepository audit;
    private final PriorityService priorityService;
    private final SlaService slaService;
    private final Clock clock;

    public HistoricalIncidentSeeder(IncidentRepository incidents, EmployeeRepository employees,
                                    TechnicianRepository technicians, ITServiceRepository services,
                                    CategoryRepository categories, AuditEntryRepository audit,
                                    PriorityService priorityService, SlaService slaService, Clock clock) {
        this.incidents = incidents;
        this.employees = employees;
        this.technicians = technicians;
        this.services = services;
        this.categories = categories;
        this.audit = audit;
        this.priorityService = priorityService;
        this.slaService = slaService;
        this.clock = clock;
    }

    /** Creates the history; returns how many incidents were created. */
    public int seed() {
        Instant now = Instant.now(clock);
        long sequence = IncidentNumber.FIRST_SEQUENCE - HISTORY.size() - 1; // keep INC1001+ for live demo
        int hour = 0;
        for (Past p : HISTORY) {
            Instant created = now.minus(Duration.ofDays(p.daysAgo())).minus(Duration.ofHours(2 + (hour++ % 6)));
            create(new IncidentNumber(++sequence), p, created, now);
        }
        return HISTORY.size();
    }

    private void create(IncidentNumber number, Past p, Instant created, Instant now) {
        Employee reporter = employees.findByUsernameIgnoreCase(p.reporter()).orElseThrow();
        ITService service = services.findByNameIgnoreCase(p.service()).orElseThrow();
        Category category = categories.findByNameIgnoreCase(p.category()).orElseThrow();
        Technician tech = technicians.findByUsernameIgnoreCase(p.technician()).orElseThrow();

        Incident incident = new Incident(number, p.title(), p.description(), reporter, service, p.impact(),
                p.urgency(), Math.max(1, p.users()), created);
        PriorityResult priority = priorityService.calculate(PriorityInput.from(incident));
        SlaPolicy policy = slaService.policyFor(service, priority.priority());
        incident.classify(category, priority.priority(), priority.explanation(), policy);
        incident.assignTo(tech);

        boolean late = p.outcome() == Outcome.CLOSED_LATE;
        Instant acknowledged = created.plus(policy.responseTarget().dividedBy(2));
        incident.acknowledge(acknowledged);
        incident.startWork();
        line(incident, created, p.reporter(), "CREATED", null, IncidentStatus.NEW, "Incident created");
        line(incident, created, "system", "CLASSIFIED", IncidentStatus.NEW, IncidentStatus.CLASSIFIED,
                "Incident classified: " + priority.explanation());
        line(incident, created, "system", "ASSIGNED", IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED,
                "Technician assigned: " + tech.getUsername());
        line(incident, acknowledged, tech.getUsername(), "ACKNOWLEDGED", IncidentStatus.ASSIGNED,
                IncidentStatus.ACKNOWLEDGED, "Technician acknowledged incident");

        if (p.outcome() == Outcome.OPEN_OVERDUE) {
            incident.addNote(tech, NoteType.INVESTIGATION, "Investigating; waiting on vendor.", acknowledged.plusSeconds(600));
            incident.checkSlaBreach(now);
            incidents.save(incident);
            return;
        }
        Duration fixTime = late
                ? policy.resolutionTarget().plus(Duration.ofHours(1))
                : policy.resolutionTarget().multipliedBy(6).dividedBy(10);
        Instant resolved = created.plus(fixTime);
        incident.resolve(new Resolution(p.fix(), p.rootCause(), tech, resolved));
        line(incident, resolved, tech.getUsername(), "RESOLVED", IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED,
                "Incident resolved: " + p.fix());
        if (p.outcome() == Outcome.RESOLVED_AWAITING_VERIFY) {
            incidents.save(incident);
            return;
        }
        Instant verified = resolved.plus(Duration.ofHours(1));
        incident.verify(new Feedback(p.rating(), null, verified));
        incident.close(null, verified.plus(Duration.ofMinutes(5)));
        line(incident, verified, p.reporter(), "VERIFIED", IncidentStatus.RESOLVED, IncidentStatus.VERIFIED,
                "Resolution verified by reporter: Rating " + p.rating() + "/5");
        line(incident, verified.plus(Duration.ofMinutes(5)), p.reporter(), "CLOSED", IncidentStatus.VERIFIED,
                IncidentStatus.CLOSED, "Incident closed");
        incidents.save(incident);
    }

    private void line(Incident incident, Instant at, String actor, String action, IncidentStatus from,
                      IncidentStatus to, String description) {
        audit.save(new AuditEntry(at, actor, action, incident.getNumber().toString(), from, to, description));
    }
}
