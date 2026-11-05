package com.iitsm.web;

import com.iitsm.assignment.AssignmentService;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.user.Technician;
import com.iitsm.security.CurrentUserProvider;
import com.iitsm.service.IncidentService;
import com.iitsm.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Technician pages: the work queue (most urgent SLA first), workload and availability.
 */
@Controller
@RequestMapping("/tech")
public class TechnicianController {

    /** Open incidents first, then by nearest resolution deadline; unclassified/finished last. */
    private static final Comparator<Incident> BY_URGENCY = Comparator
            .comparing((Incident i) -> !i.getStatus().isOpen())
            .thenComparing(i -> i.getSlaRecord().getResolutionDue(), Comparator.nullsLast(Comparator.<Instant>naturalOrder()))
            .thenComparing(Incident::getCreatedAt, Comparator.reverseOrder());

    private final IncidentService incidents;
    private final AssignmentService assignment;
    private final UserService users;
    private final CurrentUserProvider currentUser;

    public TechnicianController(IncidentService incidents, AssignmentService assignment, UserService users,
                                CurrentUserProvider currentUser) {
        this.incidents = incidents;
        this.assignment = assignment;
        this.users = users;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String dashboard(Model model) {
        Technician me = currentUser.currentTechnician();
        List<Incident> queue = incidents.assignedTo(me).stream().sorted(BY_URGENCY).toList();
        model.addAttribute("queue", queue);
        model.addAttribute("activeCount", assignment.activeWorkload(me));
        model.addAttribute("me", me);
        if (me.isTeamLead()) {
            model.addAttribute("teamWorkload", assignment.workloads());
        }
        return "tech/dashboard";
    }

    @PostMapping("/availability")
    public String availability(@RequestParam boolean available, RedirectAttributes redirect) {
        users.setOwnAvailability(currentUser.currentUser(), available);
        redirect.addFlashAttribute("success", available
                ? "You are available for new incidents." : "You will not receive new incidents.");
        return "redirect:/tech";
    }
}
