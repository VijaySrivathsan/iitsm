package com.iitsm.domain.incident;

import com.iitsm.domain.Checks;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/**
 * The reporter's rating (1-5) and comment after resolution. An immutable value embedded in the
 * incident row (composition).
 */
@Embeddable
public class Feedback {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    @Column(name = "feedback_rating")
    private Integer rating;

    @Column(name = "feedback_comment", length = 1000)
    private String comment;

    @Column(name = "feedback_given_at")
    private Instant givenAt;

    protected Feedback() {
    }

    public Feedback(int rating, String comment, Instant givenAt) {
        this.rating = Checks.requireRange(rating, MIN_RATING, MAX_RATING, "Rating");
        this.comment = Checks.optionalText(comment);
        this.givenAt = Checks.requireNonNull(givenAt, "Feedback time");
    }

    public int getRating() {
        return rating == null ? 0 : rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getGivenAt() {
        return givenAt;
    }
}
