package com.iitsm.intelligence;

import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Incident;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Word-overlap (Jaccard) similarity of the normalised title + description, plus a bonus when both
 * incidents are on the same IT service. Example from the brief:
 * "Payment application unavailable" vs "Payment transaction failing" -> {payment, application, outage}
 * vs {payment, outage} = 2/3 = 0.67, + 0.2 same service = 0.87 -> possible duplicate.
 */
@Component
public class TextSimilarityDuplicateDetector implements DuplicateDetector {

    static final double SAME_SERVICE_BONUS = 0.2;

    private final TextNormalizer normalizer = new TextNormalizer();
    private final double threshold;

    public TextSimilarityDuplicateDetector(@Value("${itsm.duplicates.threshold:0.5}") double threshold) {
        this.threshold = threshold;
    }

    @Override
    public double score(String newText, ITService newService, Incident existing) {
        double text = TextNormalizer.jaccard(normalizer.normalize(newText),
                normalizer.normalize(existing.getTitle() + " " + existing.getDescription()));
        double bonus = existing.getService().equals(newService) ? SAME_SERVICE_BONUS : 0;
        return Math.min(1.0, text + bonus);
    }

    @Override
    public double threshold() {
        return threshold;
    }
}
