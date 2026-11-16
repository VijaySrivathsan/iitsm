package com.iitsm.web;

import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import java.util.List;
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
    public String search(@RequestParam(name = "number", required = false) String number,
                         @RequestParam(name = "text", required = false) String text,
                         @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(name = "status", required = false) IncidentStatus status,
                         @RequestParam(name = "priority", required = false) Priority priority,
                         @RequestParam(name = "categoryId", required = false) Long categoryId,
                         @RequestParam(name = "serviceId", required = false) Long serviceId,
                         @RequestParam(name = "technicianId", required = false) Long technicianId,
                         @RequestParam(name = "slaStatus", required = false) SlaStatus slaStatus,
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

    @GetMapping(value = "/search/export", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@RequestParam(name = "number", required = false) String number,
                                            @RequestParam(name = "text", required = false) String text,
                                            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            @RequestParam(name = "status", required = false) IncidentStatus status,
                                            @RequestParam(name = "priority", required = false) Priority priority,
                                            @RequestParam(name = "categoryId", required = false) Long categoryId,
                                            @RequestParam(name = "serviceId", required = false) Long serviceId,
                                            @RequestParam(name = "technicianId", required = false) Long technicianId,
                                            @RequestParam(name = "slaStatus", required = false) SlaStatus slaStatus) {
        User me = currentUser.currentUser();
        IncidentSearchCriteria criteria = new IncidentSearchCriteria(number, text, from, to, status, priority,
                categoryId, serviceId, technicianId, slaStatus);
        List<Incident> results = search.search(criteria, me);
        StringBuilder csv = new StringBuilder();
        csv.append("Number,Title,Status,Priority,Service,Category,Created At,Assignee,SLA Status\r\n");
        for (Incident i : results) {
            csv.append(escapeCsv(i.getNumber())).append(",")
               .append(escapeCsv(i.getTitle())).append(",")
               .append(escapeCsv(i.getStatus().name())).append(",")
               .append(escapeCsv(i.getPriority() != null ? i.getPriority().name() : "")).append(",")
               .append(escapeCsv(i.getService().getName())).append(",")
               .append(escapeCsv(i.getCategory() != null ? i.getCategory().getName() : "")).append(",")
               .append(escapeCsv(i.getCreatedAt().toString())).append(",")
               .append(escapeCsv(i.getAssignee() != null ? i.getAssignee().getFullName() : "")).append(",")
               .append(escapeCsv(i.getSlaRecord().getStatus().name())).append("\r\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"incidents-export.csv\"")
                .body(csv.toString());
    }

    private static String escapeCsv(Object val) {
        if (val == null) return "";
        String s = val.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
