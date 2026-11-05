package com.iitsm.service;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Feedback;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentFactory;
import com.iitsm.domain.incident.IncidentNumber;
import com.iitsm.domain.incident.IncidentOperation;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.NoteType;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.incident.Resolution;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.sla.SlaPolicy;
import com.iitsm.domain.sla.SlaStatus;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.User;
import com.iitsm.assignment.AssignmentService;
import com.iitsm.event.IncidentEvent;
import com.iitsm.event.IncidentEventPublisher;
import com.iitsm.intelligence.DuplicateDetectionService;
import com.iitsm.event.IncidentEventType;
import com.iitsm.exception.CategoryNotFoundException;
import com.iitsm.exception.IncidentNotFoundException;
import com.iitsm.exception.ServiceNotFoundException;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.exception.UserNotFoundException;
import com.iitsm.priority.PriorityInput;
import com.iitsm.priority.PriorityResult;
import com.iitsm.priority.PriorityService;
import com.iitsm.sla.SlaService;
import com.iitsm.repository.CategoryRepository;
import com.iitsm.repository.ITServiceRepository;
import com.iitsm.repository.IncidentRepository;
import com.iitsm.repository.TechnicianRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Application service for the incident use cases (create, view, update, assign, resolve, close).
 *
 * <p>Responsibilities are split deliberately:
 * <ul>
 *   <li>the <b>State</b> objects decide whether a transition is legal;</li>
 *   <li>this service decides <b>who</b> may perform it (authorisation) and persists the result;</li>
 *   <li>after every change it publishes an {@link IncidentEvent} (<b>Observer</b>) instead of calling
 *       audit or notification code directly.</li>
 * </ul>
 */
@Service
@Transactional
public class IncidentService {

    private final IncidentRepository incidents;
    private final ITServiceRepository services;
    private final CategoryRepository categories;
    private final TechnicianRepository technicians;
    private final IncidentFactory incidentFactory;
    private final PriorityService priorityService;
    private final SlaService slaService;
    private final AssignmentService assignmentService;
    private final DuplicateDetectionService duplicates;
    private final IncidentEventPublisher events;
    private final Clock clock;

    public IncidentService(IncidentRepository incidents, ITServiceRepository services,
                           CategoryRepository categories, TechnicianRepository technicians,
                           IncidentFactory incidentFactory, PriorityService priorityService, SlaService slaService,
                           AssignmentService assignmentService, DuplicateDetectionService duplicates,
                           IncidentEventPublisher events, Clock clock) {
        this.incidents = incidents;
        this.services = services;
        this.categories = categories;
        this.technicians = technicians;
        this.incidentFactory = incidentFactory;
        this.priorityService = priorityService;
        this.slaService = slaService;
        this.assignmentService = assignmentService;
        this.duplicates = duplicates;
        this.events = events;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ create & view

    /**
     * An employee reports a new incident. If they chose a category, the system classifies it straight
     * away (priority calculated, SLA started); otherwise it waits as NEW for an administrator.
     */
    public Incident report(User actor, Long serviceId, Long categoryId, IncidentReport report) {
        if (!(actor instanceof Employee reporter)) {
            throw new UnauthorizedOperationException("Only employees can report incidents");
        }
        ITService service = services.findById(serviceId).orElseThrow(() -> new ServiceNotFoundException(serviceId));
        Category category = categoryId == null ? null : findCategory(categoryId);
        duplicates.rejectRepeatedReport(reporter, service, report.title(), report.description());
        IncidentNumber number = IncidentNumber.next(incidents.findMaxSequenceNo());
        Incident incident = incidents.save(incidentFactory.create(number, report, reporter, service));
        publish(IncidentEventType.CREATED, incident, null, actor,
                "Reported by " + actor.getUsername() + " for service " + service.getName());
        duplicates.flagPossibleDuplicate(incident);
        if (category != null) {
            applyClassification(incident, category, null); // automatic, done by the system
            assignmentService.autoAssign(incident);
        }
        return incident;
    }

    /** Loads an incident if the viewer is allowed to see it. */
    @Transactional(readOnly = true)
    public Incident view(String incidentNumber, User viewer) {
        Incident incident = find(incidentNumber);
        if (!canView(incident, viewer)) {
            throw new UnauthorizedOperationException("You are not allowed to view " + incidentNumber);
        }
        return incident;
    }

    /** Employees see their own incidents, technicians their assigned ones (team leads see all). */
    public boolean canView(Incident incident, User viewer) {
        if (viewer instanceof Administrator) {
            return true;
        }
        if (viewer instanceof Technician technician) {
            return technician.isTeamLead() || incident.isAssignedTo(technician);
        }
        return incident.isReportedBy(viewer);
    }

    @Transactional(readOnly = true)
    public List<Incident> reportedBy(Employee reporter) {
        return incidents.findByReporterOrderByCreatedAtDesc(reporter);
    }

    @Transactional(readOnly = true)
    public List<Incident> assignedTo(Technician technician) {
        return incidents.findByAssigneeOrderByCreatedAtDesc(technician);
    }

    @Transactional(readOnly = true)
    public List<Incident> all(User actor) {
        requireAdmin(actor, "list all incidents");
        return incidents.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }


    /**
     * Which actions this user can take on this incident now: the State pattern says what is legal in
     * the current status, and the same role rules the action methods enforce say who may do it.
     */
    @Transactional(readOnly = true)
    public Set<IncidentAction> availableActions(Incident incident, User user) {
        Set<IncidentAction> actions = EnumSet.noneOf(IncidentAction.class);
        boolean admin = user instanceof Administrator;
        boolean reporter = incident.isReportedBy(user);
        boolean assignee = incident.isAssignedTo(user);
        boolean open = incident.getStatus() != IncidentStatus.CLOSED;

        if (admin && incident.allows(IncidentOperation.CLASSIFY)) {
            actions.add(IncidentAction.CLASSIFY);
        }
        if (admin && incident.allows(IncidentOperation.ASSIGN)) {
            actions.add(IncidentAction.ASSIGN);
        }
        if (assignee) {
            addIfAllowed(actions, incident, IncidentOperation.ACKNOWLEDGE, IncidentAction.ACKNOWLEDGE);
            addIfAllowed(actions, incident, IncidentOperation.START_WORK, IncidentAction.START_WORK);
            addIfAllowed(actions, incident, IncidentOperation.REQUEST_INFO, IncidentAction.REQUEST_INFO);
            addIfAllowed(actions, incident, IncidentOperation.RESOLVE, IncidentAction.RESOLVE);
        }
        if (reporter) {
            addIfAllowed(actions, incident, IncidentOperation.PROVIDE_INFO, IncidentAction.PROVIDE_INFO);
            addIfAllowed(actions, incident, IncidentOperation.REOPEN, IncidentAction.REOPEN);
        }
        if (reporter || admin) {
            addIfAllowed(actions, incident, IncidentOperation.VERIFY, IncidentAction.VERIFY);
            addIfAllowed(actions, incident, IncidentOperation.CLOSE, IncidentAction.CLOSE);
            if (incident.getStatus().isOpen() && incident.getStatus() != IncidentStatus.NEW) {
                actions.add(IncidentAction.CHANGE_IMPACT);
            }
        }
        if (open && (reporter || assignee)) {
            actions.add(IncidentAction.ADD_NOTE);
        }
        boolean lead = user instanceof Technician t && t.isTeamLead();
        if (incident.hasUnconfirmedDuplicate() && (admin || assignee || lead)) {
            actions.add(IncidentAction.REVIEW_DUPLICATE);
        }
        return actions;
    }

    private static void addIfAllowed(Set<IncidentAction> actions, Incident incident, IncidentOperation operation,
                                     IncidentAction action) {
        if (incident.allows(operation)) {
            actions.add(action);
        }
    }

    // ------------------------------------------------------------------ classify & assign

    /** An administrator classifies (or reclassifies) an incident; priority is always calculated. */
    public Incident classify(String incidentNumber, Long categoryId, User actor) {
        requireAdmin(actor, "classify incidents");
        Incident incident = find(incidentNumber);
        boolean firstClassification = incident.getStatus() == IncidentStatus.NEW;
        applyClassification(incident, findCategory(categoryId), actor);
        if (firstClassification) {
            assignmentService.autoAssign(incident);
        }
        return incident;
    }

    /** Category + calculated priority (active PriorityStrategy) + matching SLA policy. */
    private void applyClassification(Incident incident, Category category, User actor) {
        PriorityResult result = priorityService.calculate(PriorityInput.from(incident));
        SlaPolicy policy = slaService.policyFor(incident.getService(), result.priority());
        IncidentStatus before = incident.getStatus();
        incident.classify(category, result.priority(), result.explanation(), policy);
        publish(IncidentEventType.CLASSIFIED, incident, before, actor,
                "Classified as " + category.getName() + ", " + result.explanation());
    }

    /**
     * The reporter or an administrator changes impact/urgency; priority and SLA deadlines are
     * recalculated.
     */
    public Incident updateImpactAndUrgency(String incidentNumber, Impact impact, Urgency urgency, User actor) {
        Incident incident = find(incidentNumber);
        requireReporterOrAdmin(incident, actor, "change the impact of");
        Priority oldPriority = incident.getPriority();
        PriorityResult result = priorityService.calculate(PriorityInput.from(incident).with(impact, urgency));
        SlaPolicy policy = slaService.policyFor(incident.getService(), result.priority());
        SlaStatus slaBefore = incident.getSlaRecord().getStatus();
        boolean changed = incident.reprioritise(impact, urgency, result.priority(), result.explanation(), policy);
        publish(IncidentEventType.PRIORITY_CHANGED, incident, incident.getStatus(), actor,
                changed ? "Priority changed from " + oldPriority + ": " + result.explanation()
                        : "Impact/urgency updated, priority unchanged: " + result.explanation());
        publishIfSlaNewlyBreached(incident, slaBefore);
        return incident;
    }

    /** An administrator assigns (or reassigns) a specific technician. */
    public Incident assign(String incidentNumber, Long technicianId, User actor) {
        requireAdmin(actor, "assign incidents");
        Incident incident = find(incidentNumber);
        Technician technician = technicians.findById(technicianId)
                .orElseThrow(() -> new UserNotFoundException(technicianId));
        assignmentService.assign(incident, technician, actor);
        return incident;
    }

    // ------------------------------------------------------------------ technician work

    public Incident acknowledge(String incidentNumber, User actor) {
        Incident incident = find(incidentNumber);
        requireAssignee(incident, actor, "acknowledge");
        IncidentStatus before = incident.getStatus();
        SlaStatus slaBefore = incident.getSlaRecord().getStatus();
        incident.acknowledge(now());
        publish(IncidentEventType.ACKNOWLEDGED, incident, before, actor, null);
        publishIfSlaNewlyBreached(incident, slaBefore);
        return incident;
    }

    public Incident startWork(String incidentNumber, User actor) {
        Incident incident = find(incidentNumber);
        requireAssignee(incident, actor, "start work on");
        IncidentStatus before = incident.getStatus();
        incident.startWork();
        publish(IncidentEventType.WORK_STARTED, incident, before, actor, null);
        return incident;
    }

    public Incident requestInfo(String incidentNumber, String question, User actor) {
        Incident incident = find(incidentNumber);
        Technician technician = requireAssignee(incident, actor, "request information on");
        IncidentStatus before = incident.getStatus();
        incident.requestInfo(technician, question, now());
        publish(IncidentEventType.INFO_REQUESTED, incident, before, actor, question);
        return incident;
    }

    public Incident resolve(String incidentNumber, String summary, String rootCause, User actor) {
        Incident incident = find(incidentNumber);
        Technician technician = requireAssignee(incident, actor, "resolve");
        IncidentStatus before = incident.getStatus();
        SlaStatus slaBefore = incident.getSlaRecord().getStatus();
        incident.resolve(new Resolution(summary, rootCause, technician, now()));
        publish(IncidentEventType.RESOLVED, incident, before, actor, summary);
        publishIfSlaNewlyBreached(incident, slaBefore);
        return incident;
    }

    // ------------------------------------------------------------------ reporter actions

    public Incident provideInfo(String incidentNumber, String answer, User actor) {
        Incident incident = find(incidentNumber);
        requireReporter(incident, actor, "provide information on");
        IncidentStatus before = incident.getStatus();
        incident.provideInfo(actor, answer, now());
        publish(IncidentEventType.INFO_PROVIDED, incident, before, actor, answer);
        return incident;
    }

    /**
     * Adds a note without changing status: the assigned technician adds an investigation note, the
     * reporter adds extra information.
     */
    public Incident addNote(String incidentNumber, String text, User actor) {
        Incident incident = find(incidentNumber);
        NoteType type;
        if (incident.isAssignedTo(actor)) {
            type = NoteType.INVESTIGATION;
        } else if (incident.isReportedBy(actor)) {
            type = NoteType.INFO_RESPONSE;
        } else {
            throw new UnauthorizedOperationException("Only the reporter or the assigned technician can add notes");
        }
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new UnauthorizedOperationException("Closed incidents cannot be changed");
        }
        incident.addNote(actor, type, text, now());
        publish(IncidentEventType.NOTE_ADDED, incident, incident.getStatus(), actor, text);
        return incident;
    }

    /** Reporter (or an administrator) confirms the fix. Rating is optional. */
    public Incident verify(String incidentNumber, Integer rating, String comment, User actor) {
        Incident incident = find(incidentNumber);
        requireReporterOrAdmin(incident, actor, "verify");
        IncidentStatus before = incident.getStatus();
        Feedback feedback = rating == null ? null : new Feedback(rating, comment, now());
        incident.verify(feedback);
        publish(IncidentEventType.VERIFIED, incident, before, actor,
                feedback == null ? null : "Rating " + feedback.getRating() + "/5");
        return incident;
    }

    public Incident reopen(String incidentNumber, String reason, User actor) {
        Incident incident = find(incidentNumber);
        requireReporter(incident, actor, "reopen");
        IncidentStatus before = incident.getStatus();
        SlaStatus slaBefore = incident.getSlaRecord().getStatus();
        incident.reopen(actor, reason, now());
        publish(IncidentEventType.REOPENED, incident, before, actor, reason);
        publishIfSlaNewlyBreached(incident, slaBefore);
        return incident;
    }

    public Incident close(String incidentNumber, String closureNote, User actor) {
        Incident incident = find(incidentNumber);
        requireReporterOrAdmin(incident, actor, "close");
        IncidentStatus before = incident.getStatus();
        incident.close(closureNote, now());
        publish(IncidentEventType.CLOSED, incident, before, actor, closureNote);
        return incident;
    }

    // ------------------------------------------------------------------ duplicates (feature A)

    /** The assigned technician, a team lead or an administrator confirms the suggested duplicate. */
    public Incident confirmDuplicate(String incidentNumber, User actor) {
        Incident incident = find(incidentNumber);
        requireDuplicateReviewer(incident, actor);
        Incident parent = incident.getPossibleDuplicateOf();
        incident.confirmDuplicate();
        publish(IncidentEventType.DUPLICATE_CONFIRMED, incident, incident.getStatus(), actor,
                "Confirmed as duplicate of " + (parent == null ? "?" : parent.getNumber()));
        return incident;
    }

    public Incident dismissDuplicate(String incidentNumber, User actor) {
        Incident incident = find(incidentNumber);
        requireDuplicateReviewer(incident, actor);
        incident.dismissPossibleDuplicate();
        publish(IncidentEventType.NOTE_ADDED, incident, incident.getStatus(), actor,
                "Duplicate suggestion dismissed");
        return incident;
    }

    private void requireDuplicateReviewer(Incident incident, User actor) {
        boolean allowed = actor instanceof Administrator
                || incident.isAssignedTo(actor)
                || (actor instanceof Technician t && t.isTeamLead());
        if (!allowed) {
            throw new UnauthorizedOperationException("Only the assigned technician, a team lead or an "
                    + "administrator can review duplicates");
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Publishes SLA_VIOLATED (by the system) if the last action pushed the incident into breach. */
    private void publishIfSlaNewlyBreached(Incident incident, SlaStatus before) {
        if (before != SlaStatus.BREACHED && incident.getSlaRecord().getStatus() == SlaStatus.BREACHED) {
            publish(IncidentEventType.SLA_VIOLATED, incident, incident.getStatus(), null,
                    "SLA breached for " + incident.getPriority() + " incident");
        }
    }

    private Category findCategory(Long categoryId) {
        return categories.findById(categoryId).orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private Incident find(String incidentNumber) {
        IncidentNumber number = IncidentNumber.parse(incidentNumber);
        return incidents.findBySequenceNo(number.sequence())
                .orElseThrow(() -> new IncidentNotFoundException(number.toString()));
    }

    private void requireAdmin(User actor, String what) {
        if (!(actor instanceof Administrator)) {
            throw new UnauthorizedOperationException("Only administrators can " + what);
        }
    }

    private Technician requireAssignee(Incident incident, User actor, String what) {
        if (!(actor instanceof Technician technician) || !incident.isAssignedTo(technician)) {
            throw new UnauthorizedOperationException(
                    "Only the assigned technician can " + what + " " + incident.getNumber());
        }
        return technician;
    }

    private void requireReporter(Incident incident, User actor, String what) {
        if (!incident.isReportedBy(actor)) {
            throw new UnauthorizedOperationException("Only the reporter can " + what + " " + incident.getNumber());
        }
    }

    private void requireReporterOrAdmin(Incident incident, User actor, String what) {
        if (!(actor instanceof Administrator) && !incident.isReportedBy(actor)) {
            throw new UnauthorizedOperationException(
                    "Only the reporter or an administrator can " + what + " " + incident.getNumber());
        }
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private void publish(IncidentEventType type, Incident incident, IncidentStatus before, User actor,
                         String description) {
        events.publish(type, incident, before, actor, description);
    }
}
