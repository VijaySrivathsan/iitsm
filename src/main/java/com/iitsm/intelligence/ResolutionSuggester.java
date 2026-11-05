package com.iitsm.intelligence;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Intelligent feature G - finds up to three similar incidents that were already resolved and shows
 * how they were fixed, so a technician can reuse past knowledge. Similarity is the same word overlap
 * as duplicate detection, plus a bonus for the same category.
 */
@Service
@Transactional(readOnly = true)
public class ResolutionSuggester {

    static final double MIN_SIMILARITY = 0.2;
    static final double SAME_CATEGORY_BONUS = 0.1;
    static final int MAX_SUGGESTIONS = 3;

    private static final Set<IncidentStatus> FINISHED =
            EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED);

    private final IncidentRepository incidents;
    private final TextNormalizer normalizer = new TextNormalizer();

    public ResolutionSuggester(IncidentRepository incidents) {
        this.incidents = incidents;
    }

    public List<ResolutionSuggestion> suggestFor(Incident incident) {
        Set<String> words = normalizer.normalize(incident.getTitle() + " " + incident.getDescription());
        return incidents.findByStatusInOrderByCreatedAtAsc(FINISHED).stream()
                .filter(past -> !past.equals(incident) && past.getResolution() != null)
                .map(past -> new ResolutionSuggestion(past, score(words, incident, past)))
                .filter(s -> s.similarity() >= MIN_SIMILARITY)
                .sorted(Comparator.comparingDouble(ResolutionSuggestion::similarity).reversed())
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    private double score(Set<String> words, Incident current, Incident past) {
        double text = TextNormalizer.jaccard(words,
                normalizer.normalize(past.getTitle() + " " + past.getDescription()));
        boolean sameCategory = current.getCategory() != null
                && Objects.equals(current.getCategory(), past.getCategory());
        return Math.min(1.0, text + (sameCategory ? SAME_CATEGORY_BONUS : 0));
    }
}
