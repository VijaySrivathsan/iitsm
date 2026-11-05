package com.iitsm.intelligence;

import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.user.Employee;
import com.iitsm.event.IncidentEventPublisher;
import com.iitsm.event.IncidentEventType;
import com.iitsm.exception.DuplicateIncidentException;
import com.iitsm.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Intelligent features A (duplicate detection) and B (incident correlation).
 * <ul>
 *   <li>Before saving: refuses an employee's exact repeat of their own report within an hour.</li>
 *   <li>After saving: compares the new incident with open incidents from the last 24 hours and flags the
 *       most similar one as a <i>possible duplicate</i> (a technician or admin can confirm or dismiss).</li>
 *   <li>Correlation: lists other open incidents on the same service, so related reports are seen together.</li>
 * </ul>
 */
@Service
@Transactional
public class DuplicateDetectionService {

    static final Duration LOOKBACK = Duration.ofHours(24);
    static final Duration REPEAT_WINDOW = Duration.ofHours(1);
    static final double REPEAT_SCORE = 0.9;

    private static final Set<IncidentStatus> OPEN = EnumSet.of(IncidentStatus.NEW, IncidentStatus.CLASSIFIED,
            IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS,
            IncidentStatus.PENDING_INFO);

    private final IncidentRepository incidents;
    private final DuplicateDetector detector;
    private final IncidentEventPublisher events;
    private final Clock clock;

    public DuplicateDetectionService(IncidentRepository incidents, DuplicateDetector detector,
                                     IncidentEventPublisher events, Clock clock) {
        this.incidents = incidents;
        this.detector = detector;
        this.events = events;
        this.clock = clock;
    }

    /** Throws if this reporter already reported practically the same thing on this service recently. */
    @Transactional(readOnly = true)
    public void rejectRepeatedReport(Employee reporter, ITService service, String title, String description) {
        Instant since = Instant.now(clock).minus(REPEAT_WINDOW);
        String text = text(title, description);
        incidents.findByReporterOrderByCreatedAtDesc(reporter).stream()
                .filter(i -> OPEN.contains(i.getStatus()) && i.getCreatedAt().isAfter(since))
                .filter(i -> i.getService().equals(service))
                .filter(i -> detector.score(text, service, i) >= REPEAT_SCORE)
                .findFirst()
                .ifPresent(i -> {
                    throw new DuplicateIncidentException(i.getNumber().toString());
                });
    }

    /** Flags the newly saved incident if it looks like an existing open one. */
    public Optional<DuplicateMatch> flagPossibleDuplicate(Incident incident) {
        Optional<DuplicateMatch> best = findBestMatch(incident);
        best.ifPresent(match -> {
            incident.markPossibleDuplicate(match.incident(), match.score());
            events.publish(IncidentEventType.DUPLICATE_DETECTED, incident, incident.getStatus(), null,
                    String.format(Locale.ROOT, "Possible duplicate of %s (similarity %.2f)",
                            match.incident().getNumber(), match.score()));
        });
        return best;
    }

    @Transactional(readOnly = true)
    public Optional<DuplicateMatch> findBestMatch(Incident incident) {
        String text = text(incident.getTitle(), incident.getDescription());
        return recentOpen().stream()
                .filter(other -> !other.equals(incident) && other.getDuplicateOf() == null)
                .map(other -> new DuplicateMatch(other, detector.score(text, incident.getService(), other)))
                .filter(m -> m.score() >= detector.threshold())
                .max(Comparator.comparingDouble(DuplicateMatch::score))
                .map(m -> new DuplicateMatch(original(m.incident()), m.score()));
    }

    /** If the match is itself a (possible) duplicate, point at the original report instead. */
    private static Incident original(Incident incident) {
        if (incident.getDuplicateOf() != null) {
            return incident.getDuplicateOf();
        }
        return incident.getPossibleDuplicateOf() != null ? incident.getPossibleDuplicateOf() : incident;
    }

    /** Correlation: other open incidents on the same service from the last 24 hours, newest first. */
    @Transactional(readOnly = true)
    public List<Incident> relatedOnSameService(Incident incident) {
        return recentOpen().stream()
                .filter(other -> !other.equals(incident) && other.getService().equals(incident.getService()))
                .sorted(Comparator.comparing(Incident::getCreatedAt).reversed())
                .toList();
    }

    /** Incidents confirmed as duplicates of this one. */
    @Transactional(readOnly = true)
    public List<Incident> confirmedDuplicatesOf(Incident parent) {
        return incidents.findByDuplicateOf(parent);
    }

    private List<Incident> recentOpen() {
        return incidents.findByStatusInAndCreatedAtAfter(OPEN, Instant.now(clock).minus(LOOKBACK));
    }

    private static String text(String title, String description) {
        return (title == null ? "" : title) + " " + (description == null ? "" : description);
    }
}
