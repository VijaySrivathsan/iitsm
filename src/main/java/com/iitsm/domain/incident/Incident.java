package com.iitsm.domain.incident;

import com.iitsm.domain.BaseEntity;
import com.iitsm.domain.Checks;
import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.sla.SlaRecord;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The central aggregate: one reported IT problem and everything that happens to it.
 *
 * <p>Relationships:
 * <ul>
 *   <li><b>Association</b> with {@link Employee} (reporter), {@link Technician} (assignee),
 *       {@link ITService} and {@link Category}: those objects exist independently.</li>
 *   <li><b>Composition</b> with {@link SlaRecord}, {@link Resolution}, {@link Feedback} (embedded values)
 *       and the list of {@link InvestigationNote}s (cascade + orphan removal): they live and die with
 *       the incident.</li>
 * </ul>
 *
 * <p>There are no public setters. Lifecycle operations (classify, assign, resolve, ...) are added in
 * Milestone 3 and delegate to the State pattern, so illegal transitions are impossible.
 */
@Entity
@Table(name = "incident")
public class Incident extends BaseEntity {

    public static final int MAX_TITLE_LENGTH = 150;

    @Column(nullable = false, unique = true)
    private long sequenceNo;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reporter_id")
    private Employee reporter;

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_id")
    private ITService service;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgency urgency;

    private int affectedUsers;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private Priority priority;

    @Column(length = 500)
    private String priorityExplanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private Technician assignee;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant closedAt;

    @Column(length = 1000)
    private String closureNote;

    @Embedded
    private SlaRecord slaRecord = SlaRecord.notStarted();

    @Embedded
    private Resolution resolution;

    @Embedded
    private Feedback feedback;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "incident_id")
    @OrderBy("createdAt ASC")
    private List<InvestigationNote> notes = new ArrayList<>();

    /** Possible duplicate found by the duplicate detector (Milestone 8); not yet confirmed. */
    @ManyToOne
    @JoinColumn(name = "possible_duplicate_of_id")
    private Incident possibleDuplicateOf;

    private Double similarityScore;

    /** Confirmed duplicate: this incident is a child of the given parent incident. */
    @ManyToOne
    @JoinColumn(name = "duplicate_of_id")
    private Incident duplicateOf;

    protected Incident() {
    }

    /**
     * Creates a NEW incident. Normally called by IncidentFactory (Milestone 3), which also assigns
     * the next incident number.
     */
    public Incident(IncidentNumber number, String title, String description, Employee reporter, ITService service,
                    Impact impact, Urgency urgency, int affectedUsers, Instant createdAt) {
        this.sequenceNo = Checks.requireNonNull(number, "Incident number").sequence();
        this.title = requireTitle(title);
        this.description = Checks.requireText(description, "Description");
        this.reporter = Checks.requireNonNull(reporter, "Reporter");
        this.service = Checks.requireNonNull(service, "Affected service");
        this.impact = Checks.requireNonNull(impact, "Impact");
        this.urgency = Checks.requireNonNull(urgency, "Urgency");
        this.affectedUsers = Checks.requireRange(affectedUsers, 1, 1_000_000, "Affected users");
        this.createdAt = Checks.requireNonNull(createdAt, "Creation time");
        this.status = IncidentStatus.NEW;
    }

    /** Adds an entry to the investigation log. The incident creates and owns the note. */
    public InvestigationNote addNote(User author, NoteType type, String text, Instant at) {
        InvestigationNote note = new InvestigationNote(author, type, text, at);
        notes.add(note);
        return note;
    }

    /** True if the given user reported this incident. */
    public boolean isReportedBy(User user) {
        return user != null && reporter.equals(user);
    }

    /** True if the given user is the technician currently assigned. */
    public boolean isAssignedTo(User user) {
        return user != null && assignee != null && assignee.equals(user);
    }

    private static String requireTitle(String title) {
        String value = Checks.requireText(title, "Title");
        return value.length() > MAX_TITLE_LENGTH ? value.substring(0, MAX_TITLE_LENGTH) : value;
    }

    // ----- read-only accessors -----

    public IncidentNumber getNumber() {
        return new IncidentNumber(sequenceNo);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Employee getReporter() {
        return reporter;
    }

    public ITService getService() {
        return service;
    }

    public Category getCategory() {
        return category;
    }

    public Impact getImpact() {
        return impact;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public int getAffectedUsers() {
        return affectedUsers;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getPriorityExplanation() {
        return priorityExplanation;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public Technician getAssignee() {
        return assignee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public String getClosureNote() {
        return closureNote;
    }

    public SlaRecord getSlaRecord() {
        return slaRecord;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    /** Read-only view: notes can only be added through {@link #addNote}. */
    public List<InvestigationNote> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    public Incident getPossibleDuplicateOf() {
        return possibleDuplicateOf;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public Incident getDuplicateOf() {
        return duplicateOf;
    }

    @Override
    public String toString() {
        return getNumber() + " [" + status + "] " + title;
    }
}
