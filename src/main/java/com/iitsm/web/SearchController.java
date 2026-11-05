package com.iitsm.web;

import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.sla.SlaStatus;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.User;
import com.iitsm.security.CurrentUserProvider;
import com.iitsm.service.CatalogService;
import com.iitsm.service.IncidentSearchCriteria;
import com.iitsm.service.SearchService;
import com.iitsm.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Incident search for every role (results limited to what the user may see).
 */
@Controller
public class SearchController {

    private final SearchService search;
    private final CatalogService catalog;
    private final UserService users;
    private final CurrentUserProvider currentUser;

    public SearchController(SearchService search, CatalogService catalog, UserService users,
                            CurrentUserProvider currentUser) {
        this.search = search;
        this.catalog = catalog;
        this.users = users;
        this.currentUser = currentUser;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String number,
                         @RequestParam(required = false) String text,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(required = false) IncidentStatus status,
                         @RequestParam(required = false) Priority priority,
                         @RequestParam(required = false) Long categoryId,
                         @RequestParam(required = false) Long serviceId,
                         @RequestParam(required = false) Long technicianId,
                         @RequestParam(required = false) SlaStatus slaStatus,
                         Model model) {
        User me = currentUser.currentUser();
        IncidentSearchCriteria criteria = new IncidentSearchCriteria(number, text, from, to, status, priority,
                categoryId, serviceId, technicianId, slaStatus);
        model.addAttribute("c", criteria);
        model.addAttribute("results", search.search(criteria, me));
        model.addAttribute("statuses", IncidentStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("slaStatuses", SlaStatus.values());
        model.addAttribute("categories", catalog.allCategories());
        model.addAttribute("services", catalog.allServices());
        boolean seesAll = me instanceof Administrator || (me instanceof Technician t && t.isTeamLead());
        if (seesAll) {
            model.addAttribute("technicians", users.allTechnicians());
        }
        return "search";
    }
}
