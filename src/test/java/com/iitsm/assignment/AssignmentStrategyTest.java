package com.iitsm.assignment;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentNumber;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.sla.SlaPolicy;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests of the four strategies (no Spring, no database).
 */
class AssignmentStrategyTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    private final Category database = new Category("Database", null, null);
    private final Category network = new Category("Network", null, null);

    private Technician dbSenior;   // database skill, experience 5
    private Technician dbJunior;   // database skill, experience 2
    private Technician netTech;    // network skill only, experience 3

    private Incident dbIncident;

    @BeforeEach
    void setUp() {
        dbSenior = tech("alice", 5, database);
        dbJunior = tech("bob", 2, database);
        netTech = tech("carol", 3, network);
        dbIncident = incident(database, Priority.P1);
    }

    private static Technician tech(String username, int experience, Category skill) {
        Technician t = new Technician(username, "h", username, null, null, 5, experience);
        t.addSkill(skill);
        return t;
    }

    private static Incident incident(Category category, Priority priority) {
        Employee reporter = new Employee("emp1", "h", "Asha", null, null, null);
        Incident incident = new Incident(new IncidentNumber(1001), "DB slow", "Queries time out", reporter,
                new ITService("Core Database", null, 4), Impact.HIGH, Urgency.HIGH, 50, NOW);
        incident.classify(category, priority, null, SlaPolicy.defaultFor(priority, 5, 30));
        return incident;
    }

    private static List<String> names(List<TechnicianScore> ranking) {
        return ranking.stream().map(s -> s.technician().getUsername()).toList();
    }

    // ---------------------------------------------------------------- skill-based

    @Test
    void skill_choosesOnlySkilledTechnicians_leastLoadedFirst() {
        SkillBasedAssignmentStrategy skill = new SkillBasedAssignmentStrategy();
        List<Candidate> candidates = List.of(
                new Candidate(dbSenior, 3, 0), new Candidate(dbJunior, 1, 0), new Candidate(netTech, 0, 0));

        assertThat(names(skill.rank(dbIncident, candidates))).containsExactly("bob", "alice");
    }

    @Test
    void skill_tieOnWorkload_goesToMoreExperienced() {
        SkillBasedAssignmentStrategy skill = new SkillBasedAssignmentStrategy();
        List<Candidate> candidates = List.of(new Candidate(dbJunior, 1, 0), new Candidate(dbSenior, 1, 0));

        assertThat(skill.select(dbIncident, candidates)).contains(dbSenior);
    }

    @Test
    void skill_noSkilledTechnician_returnsNobody() {
        SkillBasedAssignmentStrategy skill = new SkillBasedAssignmentStrategy();
        assertThat(skill.select(dbIncident, List.of(new Candidate(netTech, 0, 0)))).isEmpty();
    }

    // ---------------------------------------------------------------- load-based

    @Test
    void load_choosesLeastBusyRegardlessOfSkill() {
        LoadBasedAssignmentStrategy load = new LoadBasedAssignmentStrategy();
        List<Candidate> candidates = List.of(
                new Candidate(dbSenior, 2, 0), new Candidate(dbJunior, 4, 0), new Candidate(netTech, 0, 0));

        assertThat(names(load.rank(dbIncident, candidates))).containsExactly("carol", "alice", "bob");
    }

    // ---------------------------------------------------------------- round-robin

    @Test
    void roundRobin_takesTurnsAmongSkilledTechnicians() {
        RoundRobinAssignmentStrategy roundRobin = new RoundRobinAssignmentStrategy();
        List<Candidate> candidates = List.of(
                new Candidate(dbJunior, 0, 0), new Candidate(dbSenior, 0, 0), new Candidate(netTech, 0, 0));

        Technician first = roundRobin.select(dbIncident, candidates).orElseThrow();
        roundRobin.recordAssignment(dbIncident, first);
        Technician second = roundRobin.select(dbIncident, candidates).orElseThrow();
        roundRobin.recordAssignment(dbIncident, second);
        Technician third = roundRobin.select(dbIncident, candidates).orElseThrow();

        assertThat(List.of(first, second, third)).containsExactly(dbSenior, dbJunior, dbSenior);
    }

    @Test
    void roundRobin_rankingDoesNotAdvanceTheTurn() {
        RoundRobinAssignmentStrategy roundRobin = new RoundRobinAssignmentStrategy();
        List<Candidate> candidates = List.of(new Candidate(dbJunior, 0, 0), new Candidate(dbSenior, 0, 0));
        roundRobin.rank(dbIncident, candidates);
        roundRobin.rank(dbIncident, candidates);
        assertThat(roundRobin.select(dbIncident, candidates)).contains(dbSenior); // "alice" still first
    }

    // ---------------------------------------------------------------- smart (feature D)

    @Test
    void smart_prefersSkilledSeniorWithServiceHistory() {
        SmartRecommendationStrategy smart = new SmartRecommendationStrategy();
        List<Candidate> candidates = List.of(
                new Candidate(netTech, 0, 0),     // idle but wrong skill
                new Candidate(dbJunior, 0, 0),    // skilled, junior
                new Candidate(dbSenior, 2, 5));   // skilled, senior, busier, knows the service

        List<TechnicianScore> ranking = smart.rank(dbIncident, candidates);

        assertThat(names(ranking)).containsExactly("alice", "bob", "carol");
        // alice: 40 + 25*(1-2/5)=15 + 15 + 10 + 10 senior bonus = 90
        assertThat(ranking.get(0).score()).isEqualTo(90.0);
        assertThat(ranking.get(0).reason()).contains("senior bonus");
        assertThat(ranking.get(2).reason()).contains("no DATABASE skill");
    }

    @Test
    void smart_noSeniorBonusForLowPriority() {
        SmartRecommendationStrategy smart = new SmartRecommendationStrategy();
        Incident p4 = incident(database, Priority.P4);
        TechnicianScore score = smart.score(p4, new Candidate(dbSenior, 0, 0));
        // 40 + 25 + 15 + 0 = 80
        assertThat(score.score()).isEqualTo(80.0);
    }

    @Test
    void allStrategies_handleNoCandidates() {
        List<AssignmentStrategy> strategies = List.of(new SkillBasedAssignmentStrategy(),
                new LoadBasedAssignmentStrategy(), new RoundRobinAssignmentStrategy(),
                new SmartRecommendationStrategy());
        for (AssignmentStrategy strategy : strategies) {
            assertThat(strategy.select(dbIncident, List.of())).as(strategy.name()).isEmpty();
        }
    }

    @Test
    void candidate_loadRatio() {
        assertThat(new Candidate(dbSenior, 2, 0).loadRatio()).isEqualTo(0.4);
        User u = dbSenior; // Technician is-a User
        assertThat(u.getRole().name()).isEqualTo("TECHNICIAN");
    }
}
