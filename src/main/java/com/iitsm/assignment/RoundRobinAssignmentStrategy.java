package com.iitsm.assignment;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.user.Technician;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Takes turns: skilled technicians (or everyone, if nobody has the skill) in a fixed order, starting
 * after whoever got the previous incident of that category. Keeps one cursor per category in memory
 * (a {@link ConcurrentHashMap} of {@link AtomicInteger}s, safe if two incidents arrive at once).
 * The cursor resets on restart, which is acceptable for a fairness rule.
 */
@Component
public class RoundRobinAssignmentStrategy implements AssignmentStrategy {

    public static final String NAME = "round-robin";

    private final Map<String, AtomicInteger> cursors = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Takes turns among technicians skilled in the category";
    }

    @Override
    public List<TechnicianScore> rank(Incident incident, List<Candidate> candidates) {
        List<Candidate> pool = pool(incident, candidates);
        if (pool.isEmpty()) {
            return List.of();
        }
        int start = Math.floorMod(cursor(incident).get(), pool.size());
        List<TechnicianScore> ranking = new ArrayList<>(pool.size());
        for (int i = 0; i < pool.size(); i++) {
            Candidate c = pool.get((start + i) % pool.size());
            ranking.add(new TechnicianScore(c.technician(), pool.size() - i, c.activeIncidents(),
                    i == 0 ? "Next in turn" : "Turn " + (i + 1)));
        }
        return ranking;
    }

    @Override
    public void recordAssignment(Incident incident, Technician technician) {
        cursor(incident).incrementAndGet();
    }

    /** Skilled candidates if there are any, otherwise all, in a stable alphabetical order. */
    private List<Candidate> pool(Incident incident, List<Candidate> candidates) {
        List<Candidate> skilled = candidates.stream()
                .filter(c -> c.technician().hasSkill(incident.getCategory()))
                .toList();
        List<Candidate> pool = new ArrayList<>(skilled.isEmpty() ? candidates : skilled);
        pool.sort(Comparator.comparing(Candidate::username));
        return pool;
    }

    private AtomicInteger cursor(Incident incident) {
        String key = incident.getCategory() == null ? "-" : incident.getCategory().getName();
        return cursors.computeIfAbsent(key, k -> new AtomicInteger());
    }
}
