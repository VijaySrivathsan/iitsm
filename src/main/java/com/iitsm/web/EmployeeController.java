package com.iitsm.web;

import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.user.Employee;
import com.iitsm.security.CurrentUserProvider;
import com.iitsm.service.CatalogService;
import com.iitsm.service.IncidentService;
import com.iitsm.web.form.ReportIncidentForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Employee pages: my incidents, and the report form.
 */
@Controller
@RequestMapping("/employee")
public class EmployeeController {

    private final IncidentService incidents;
    private final CatalogService catalog;
    private final CurrentUserProvider currentUser;

    public EmployeeController(IncidentService incidents, CatalogService catalog, CurrentUserProvider currentUser) {
        this.incidents = incidents;
        this.catalog = catalog;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String dashboard(Model model) {
        Employee me = currentUser.currentEmployee();
        model.addAttribute("incidents", incidents.reportedBy(me));
        return "employee/dashboard";
    }

    @GetMapping("/report")
    public String reportForm(Model model) {
        model.addAttribute("form", new ReportIncidentForm());
        addChoices(model);
        return "employee/report";
    }

    @PostMapping("/report")
    public String report(@Valid @ModelAttribute("form") ReportIncidentForm form, BindingResult errors, Model model,
                         RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            addChoices(model);
            return "employee/report";
        }
        Incident incident = incidents.report(currentUser.currentUser(), form.getServiceId(), form.getCategoryId(),
                new IncidentReport(form.getTitle(), form.getDescription(), form.getImpact(), form.getUrgency(),
                        form.getAffectedUsers()));
        redirect.addFlashAttribute("success", "Incident " + incident.getNumber() + " reported. Priority: "
                + (incident.getPriority() == null ? "pending classification" : incident.getPriority().display()));
        return "redirect:/incidents/" + incident.getNumber();
    }

    private void addChoices(Model model) {
        model.addAttribute("services", catalog.activeServices());
        model.addAttribute("categories", catalog.activeCategories());
        model.addAttribute("impacts", Impact.values());
        model.addAttribute("urgencies", Urgency.values());
    }
}
