package com.iitsm.priority;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Holds every {@link PriorityStrategy} Spring finds and delegates to the one named in
 * {@code itsm.priority.strategy}. Adding a new algorithm only needs a new {@code @Component}
 * implementing the interface - this class does not change (open-closed principle).
 */
@Service
public class PriorityService {

    private final Map<String, PriorityStrategy> strategies;
    private final PriorityStrategy active;

    public PriorityService(List<PriorityStrategy> strategies,
                           @Value("${itsm.priority.strategy:" + WeightedScoringPriorityStrategy.NAME + "}")
                           String activeName) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(PriorityStrategy::name, Function.identity()));
        this.active = this.strategies.get(activeName);
        if (active == null) {
            throw new IllegalStateException("Unknown priority strategy '" + activeName
                    + "'. Available: " + this.strategies.keySet());
        }
    }

    public PriorityResult calculate(PriorityInput input) {
        return active.calculate(input);
    }

    public PriorityStrategy activeStrategy() {
        return active;
    }

    public Map<String, PriorityStrategy> availableStrategies() {
        return strategies;
    }
}
