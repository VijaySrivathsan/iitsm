package com.iitsm.domain.incident;

/**
 * How widely an incident affects the business. The weight feeds the priority calculation.
 */
public enum Impact {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High");

    private final int weight;
    private final String label;

    Impact(int weight, String label) {
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
