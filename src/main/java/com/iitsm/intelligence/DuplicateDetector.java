package com.iitsm.intelligence;

import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Incident;

/**
 * Intelligent feature A - how alike a new report is to an existing incident. An interface so the
 * text-similarity algorithm can later be replaced (e.g. by an ML/embedding model) without touching
 * the code that uses it.
 */
public interface DuplicateDetector {

    /** Similarity from 0 (unrelated) to 1 (same problem). */
    double score(String newText, ITService newService, Incident existing);

    /** Scores at or above this mean "possible duplicate". */
    double threshold();
}
