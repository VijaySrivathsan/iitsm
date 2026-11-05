package com.iitsm.priority;

/**
 * Strategy pattern: an interchangeable algorithm for turning incident facts into a priority.
 * The active implementation is chosen in configuration ({@code itsm.priority.strategy}), so the
 * algorithm can be changed without touching the code that classifies incidents.
 */
public interface PriorityStrategy {

    /** Short name used in configuration, e.g. "weighted". */
    String name();

    /** One-line description shown to administrators. */
    String description();

    PriorityResult calculate(PriorityInput input);
}
