package com.iitsm.domain.catalog;

import com.iitsm.domain.BaseEntity;
import com.iitsm.domain.Checks;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * An incident category such as Network or Database. Categories are stored as data rather than a
 * Java enum, so an administrator can add a new one without changing or redeploying code
 * (open-closed principle).
 */
@Entity
@Table(name = "category")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    /** Comma-separated words used to suggest this category from an incident description. */
    @Column(length = 500)
    private String keywords;

    private boolean active = true;

    protected Category() {
    }

    public Category(String name, String description, String keywords) {
        this.name = Checks.requireText(name, "Category name").toUpperCase(Locale.ROOT);
        this.description = Checks.optionalText(description);
        this.keywords = normaliseKeywords(keywords);
    }

    public void update(String description, String keywords) {
        this.description = Checks.optionalText(description);
        this.keywords = normaliseKeywords(keywords);
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    /** Keywords as a list, lower-case and trimmed. */
    public List<String> keywordList() {
        if (keywords == null) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .toList();
    }

    private static String normaliseKeywords(String keywords) {
        String text = Checks.optionalText(keywords);
        return text == null ? null : text.toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getKeywords() {
        return keywords;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return name;
    }
}
