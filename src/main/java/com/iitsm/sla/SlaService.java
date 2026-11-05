package com.iitsm.sla;

import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.sla.SlaPolicy;
import com.iitsm.exception.MissingSlaConfigurationException;
import com.iitsm.repository.SlaPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Finds which SLA policy applies to an incident: the service's own policy for that priority if one
 * exists, otherwise the default policy for the priority. Also seeds the default policies from the
 * project brief so a fresh database is usable straight away.
 */
@Service
@Transactional
public class SlaService {

    /** Default targets from the brief, in minutes: priority -> {response, resolution}. */
    public static final Map<Priority, int[]> BRIEF_DEFAULTS = Map.of(
            Priority.P1, new int[]{5, 30},
            Priority.P2, new int[]{15, 120},
            Priority.P3, new int[]{60, 480},
            Priority.P4, new int[]{240, 1440});

    private final SlaPolicyRepository policies;

    public SlaService(SlaPolicyRepository policies) {
        this.policies = policies;
    }

    /**
     * Service-specific policy first, then the default; throws if neither exists.
     */
    @Transactional(readOnly = true)
    public SlaPolicy policyFor(ITService service, Priority priority) {
        return policies.findByServiceAndPriority(service, priority)
                .or(() -> policies.findByServiceIsNullAndPriority(priority))
                .orElseThrow(() -> new MissingSlaConfigurationException(service.getName(), priority));
    }

    /** Creates any missing default policies (P1-P4). Returns how many were created. */
    public int ensureDefaultPolicies() {
        int created = 0;
        for (Priority priority : Priority.values()) {
            if (policies.findByServiceIsNullAndPriority(priority).isEmpty()) {
                int[] targets = BRIEF_DEFAULTS.get(priority);
                policies.save(SlaPolicy.defaultFor(priority, targets[0], targets[1]));
                created++;
            }
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<SlaPolicy> allPolicies() {
        return policies.findAll();
    }
}
