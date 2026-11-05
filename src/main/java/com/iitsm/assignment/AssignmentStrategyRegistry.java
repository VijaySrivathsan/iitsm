package com.iitsm.assignment;

import com.iitsm.domain.settings.SystemSetting;
import com.iitsm.exception.InvalidInputException;
import com.iitsm.repository.SystemSettingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Knows every {@link AssignmentStrategy} (Spring injects all implementations) and which one is
 * active. The active name is stored as a {@link SystemSetting}, so an administrator's choice is
 * applied immediately and survives restarts. A new strategy only needs a new {@code @Component}.
 */
@Component
public class AssignmentStrategyRegistry {

    public static final String SETTING_KEY = "assignment.strategy";
    public static final String DEFAULT_STRATEGY = SkillBasedAssignmentStrategy.NAME;

    private final Map<String, AssignmentStrategy> strategies;
    private final SystemSettingRepository settings;

    public AssignmentStrategyRegistry(List<AssignmentStrategy> strategies, SystemSettingRepository settings) {
        this.strategies = strategies.stream().collect(Collectors.toMap(
                AssignmentStrategy::name, Function.identity(), (a, b) -> a, TreeMap::new));
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public AssignmentStrategy active() {
        String name = settings.findById(SETTING_KEY).map(SystemSetting::getValue).orElse(DEFAULT_STRATEGY);
        AssignmentStrategy strategy = strategies.get(name);
        return strategy != null ? strategy : strategies.get(DEFAULT_STRATEGY);
    }

    @Transactional
    public AssignmentStrategy activate(String name) {
        AssignmentStrategy strategy = get(name);
        settings.findById(SETTING_KEY).ifPresentOrElse(
                s -> s.changeValue(name),
                () -> settings.save(new SystemSetting(SETTING_KEY, name)));
        return strategy;
    }

    public AssignmentStrategy get(String name) {
        AssignmentStrategy strategy = strategies.get(name);
        if (strategy == null) {
            throw new InvalidInputException("Unknown assignment strategy '" + name + "'. Available: "
                    + strategies.keySet());
        }
        return strategy;
    }

    public Collection<AssignmentStrategy> all() {
        return strategies.values();
    }
}
