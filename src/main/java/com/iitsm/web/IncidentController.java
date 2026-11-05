package com.iitsm.web;

import com.iitsm.assignment.AssignmentService;
import com.iitsm.audit.AuditService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.user.User;
import com.iitsm.intelligence.DuplicateDetectionService;
import com.iitsm.intelligence.EscalationService;
import com.iitsm.security.CurrentUserProvider;
import com.iitsm.service.CatalogService;
import com.iitsm.service.IncidentAction;
import com.iitsm.service.IncidentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/**
 * The incident detail page shared by all roles, and every lifecycle action. Each action calls one
 * IncidentService method; permission and state rules are enforced there, and any error comes back
 * as a banner via GlobalExceptionHandler.
 */
@Controller
@RequestMapping("/incidents/{number}")
public class IncidentController {

    private final IncidentService incidents;
    private final AssignmentService assignment;
    private final AuditService audit;
    private final CatalogService catalog;
    private final DuplicateDetectionService duplicates;
    private final EscalationService escalation;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public IncidentController(IncidentService incidents, AssignmentService assignment, AuditService audit,
                              CatalogService catalog, DuplicateDetectionService duplicates,
                              EscalationService escalation, CurrentUserProvider currentUser, Clock clock) {
        this.incidents = incidents;
        this.assignment = assignment;
        this.audit = audit;
        this.catalog = catalog;
        this.duplicates = duplicates;
        this.escalation = escalation;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @GetMapping
    public String detail(@PathVariable String number, Model model) {
        User me = currentUser.currentUser();
        Incident incident = incidents.view(number, me);
        Set<IncidentAction> actions = incidents.availableActions(incident, me);
        model.addAttribute("incident", incident);
        model.addAttribute("actions", actions);
        model.addAttribute("can", actions.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));
        model.addAttribute("history", audit.historyOf(incident.getNumber().toString()));
        model.addAttribute("now", Instant.now(clock));
        model.addAttribute("impacts", Impact.values());
        model.addAttribute("urgencies", Urgency.values());
        if (actions.contains(IncidentAction.CLASSIFY)) {
            model.addAttribute("categories", catalog.activeCategories());
        }
        if (actions.contains(IncidentAction.ASSIGN)) {
            model.addAttribute("recommendations", assignment.recommend(incident));
        }
        // Intelligent features: correlation (B), confirmed duplicates (A) and escalations (C)
        model.addAttribute("related", duplicates.relatedOnSameService(incident).stream()
                .filter(other -> incidents.canView(other, me)).toList());
        model.addAttribute("children", duplicates.confirmedDuplicatesOf(incident));
        model.addAttribute("escalations", escalation.historyOf(incident));
        return "incidents/detail";
    }

    @PostMapping("/duplicate/confirm")
    public String confirmDuplicate(@PathVariable String number, RedirectAttributes r) {
        incidents.confirmDuplicate(number, me());
        return done(number, "Linked as a duplicate.", r);
    }

    @PostMapping("/duplicate/dismiss")
    public String dismissDuplicate(@PathVariable String number, RedirectAttributes r) {
        incidents.dismissDuplicate(number, me());
        return done(number, "Duplicate suggestion dismissed.", r);
    }

    @PostMapping("/classify")
    public String classify(@PathVariable String number, @RequestParam Long categoryId, RedirectAttributes r) {
        incidents.classify(number, categoryId, me());
        return done(number, "Incident classified.", r);
    }

    @PostMapping("/assign")
    public String assign(@PathVariable String number, @RequestParam Long technicianId, RedirectAttributes r) {
        incidents.assign(number, technicianId, me());
        return done(number, "Technician assigned.", r);
    }

    @PostMapping("/acknowledge")
    public String acknowledge(@PathVariable String number, RedirectAttributes r) {
        incidents.acknowledge(number, me());
        return done(number, "Incident acknowledged.", r);
    }

    @PostMapping("/start")
    public String start(@PathVariable String number, RedirectAttributes r) {
        incidents.startWork(number, me());
        return done(number, "Investigation started.", r);
    }

    @PostMapping("/request-info")
    public String requestInfo(@PathVariable String number, @RequestParam String question, RedirectAttributes r) {
        incidents.requestInfo(number, question, me());
        return done(number, "Question sent to the reporter.", r);
    }

    @PostMapping("/provide-info")
    public String provideInfo(@PathVariable String number, @RequestParam String answer, RedirectAttributes r) {
        incidents.provideInfo(number, answer, me());
        return done(number, "Thanks - your answer was sent to the technician.", r);
    }

    @PostMapping("/note")
    public String note(@PathVariable String number, @RequestParam String text, RedirectAttributes r) {
        incidents.addNote(number, text, me());
        return done(number, "Note added.", r);
    }

    @PostMapping("/resolve")
    public String resolve(@PathVariable String number, @RequestParam String summary,
                          @RequestParam(required = false) String rootCause, RedirectAttributes r) {
        incidents.resolve(number, summary, rootCause, me());
        return done(number, "Incident resolved. The reporter has been asked to verify.", r);
    }

    @PostMapping("/verify")
    public String verify(@PathVariable String number, @RequestParam(required = false) Integer rating,
                         @RequestParam(required = false) String comment, RedirectAttributes r) {
        incidents.verify(number, rating, comment, me());
        return done(number, "Resolution verified. Thank you for the feedback.", r);
    }

    @PostMapping("/reopen")
    public String reopen(@PathVariable String number, @RequestParam String reason, RedirectAttributes r) {
        incidents.reopen(number, reason, me());
        return done(number, "Incident reopened.", r);
    }

    @PostMapping("/close")
    public String close(@PathVariable String number, @RequestParam(required = false) String note,
                        RedirectAttributes r) {
        incidents.close(number, note, me());
        return done(number, "Incident closed.", r);
    }

    @PostMapping("/impact")
    public String impact(@PathVariable String number, @RequestParam Impact impact, @RequestParam Urgency urgency,
                         RedirectAttributes r) {
        Incident incident = incidents.updateImpactAndUrgency(number, impact, urgency, me());
        return done(number, "Priority is now " + incident.getPriority().display() + ".", r);
    }

    private User me() {
        return currentUser.currentUser();
    }

    private static String done(String number, String message, RedirectAttributes redirect) {
        redirect.addFlashAttribute("success", message);
        return "redirect:/incidents/" + number.toUpperCase();
    }
}
