package com.iitsm.domain.incident;

/**
 * How quickly the business needs an incident fixed. The weight feeds the priority calculation.
 */
public enum Urgency {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High");

    private final int weight;
    private final String label;

    Urgency(int weight, String label) {
        this.weight = weight;
        this.label = label;
    }

    public int weight() {
        return weight;
    }

    public String label() {
        return label;
    }
}
