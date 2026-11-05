package com.iitsm.web;

import com.iitsm.assignment.AssignmentService;
import com.iitsm.audit.AuditService;
import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.user.NewUser;
import com.iitsm.domain.user.Role;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.User;
import com.iitsm.event.IncidentEventType;
import com.iitsm.exception.InvalidInputException;
import com.iitsm.notification.NotificationService;
import com.iitsm.security.CurrentUserProvider;
import com.iitsm.service.CatalogService;
import com.iitsm.service.IncidentService;
import com.iitsm.service.ReportService;
import com.iitsm.service.UserService;
import com.iitsm.web.form.CategoryForm;
import com.iitsm.web.form.NewUserForm;
import com.iitsm.web.form.ServiceForm;
import com.iitsm.web.form.SlaPolicyForm;
import com.iitsm.web.form.TechnicianForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Administrator pages: overview, all incidents, users and technicians, IT services, categories,
 * SLA policies, assignment strategy, audit log and the simulated email/SMS outbox.
 * All URLs are restricted to ROLE_ADMIN by SecurityConfig.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final IncidentService incidents;
    private final AssignmentService assignment;
    private final UserService users;
    private final CatalogService catalog;
    private final AuditService audit;
    private final NotificationService notifications;
    private final ReportService reports;
    private final CurrentUserProvider currentUser;

    public AdminController(IncidentService incidents, AssignmentService assignment, UserService users,
                           CatalogService catalog, AuditService audit, NotificationService notifications,
                           ReportService reports, CurrentUserProvider currentUser) {
        this.incidents = incidents;
        this.assignment = assignment;
        this.users = users;
        this.catalog = catalog;
        this.audit = audit;
        this.notifications = notifications;
        this.reports = reports;
        this.currentUser = currentUser;
    }

    // ---------------------------------------------------------------- overview & incidents

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("report", reports.overview(me()));
        model.addAttribute("workloads", assignment.workloads());
        model.addAttribute("strategy", assignment.activeStrategy());
        return "admin/dashboard";
    }

    @GetMapping("/incidents")
    public String allIncidents(Model model) {
        model.addAttribute("incidents", incidents.all(me()));
        return "admin/incidents";
    }

    // ---------------------------------------------------------------- users & technicians

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("form", new NewUserForm());
        return usersPage(model);
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute("form") NewUserForm form, BindingResult errors, Model model,
                             RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return usersPage(model);
        }
        try {
            User user = users.createUser(form.getRole(), new NewUser(form.getUsername(), form.getPassword(),
                    form.getFullName(), form.getEmail(), form.getPhone()));
            redirect.addFlashAttribute("success", user.getRole().label() + " " + user.getUsername() + " created.");
        } catch (InvalidInputException e) {
            errors.reject("user", e.getMessage());
            return usersPage(model);
        }
        return "redirect:/admin/users";
    }

    private String usersPage(Model model) {
        model.addAttribute("users", users.allUsers());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/active")
    public String setActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes redirect) {
        User user = users.setActive(id, active, me());
        redirect.addFlashAttribute("success", user.getUsername() + (active ? " activated." : " deactivated."));
        return "redirect:/admin/users";
    }

    @GetMapping("/technicians/{id}")
    public String technician(@PathVariable Long id, Model model) {
        Technician tech = users.findTechnician(id);
        TechnicianForm form = new TechnicianForm();
        form.setSkillIds(tech.getSkills().stream().map(Category::getId).collect(Collectors.toSet()));
        form.setMaxWorkload(tech.getMaxWorkload());
        form.setExperienceLevel(tech.getExperienceLevel());
        form.setTeamLead(tech.isTeamLead());
        form.setAvailable(tech.isAvailable());
        model.addAttribute("form", form);
        return technicianPage(tech, model);
    }

    @PostMapping("/technicians/{id}")
    public String updateTechnician(@PathVariable Long id, @Valid @ModelAttribute("form") TechnicianForm form,
                                   BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return technicianPage(users.findTechnician(id), model);
        }
        Technician tech = users.updateTechnician(id, form.getSkillIds(), form.getMaxWorkload(),
                form.getExperienceLevel(), form.isTeamLead(), form.isAvailable());
        redirect.addFlashAttribute("success", "Technician " + tech.getUsername() + " updated.");
        return "redirect:/admin/users";
    }

    private String technicianPage(Technician tech, Model model) {
        model.addAttribute("tech", tech);
        model.addAttribute("categories", catalog.allCategories());
        model.addAttribute("activeCount", assignment.activeWorkload(tech));
        return "admin/technician";
    }

    // ---------------------------------------------------------------- services

    @GetMapping("/services")
    public String services(Model model) {
        model.addAttribute("form", new ServiceForm());
        model.addAttribute("services", catalog.allServices());
        return "admin/services";
    }

    @PostMapping("/services")
    public String createService(@Valid @ModelAttribute("form") ServiceForm form, BindingResult errors, Model model,
                                RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("services", catalog.allServices());
            return "admin/services";
        }
        catalog.createService(form.getName(), form.getDescription(), form.getBusinessCriticality());
        redirect.addFlashAttribute("success", "Service " + form.getName() + " added.");
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}")
    public String updateService(@PathVariable Long id, @RequestParam(required = false) String description,
                                @RequestParam int businessCriticality, @RequestParam(defaultValue = "false") boolean active,
                                RedirectAttributes redirect) {
        catalog.updateService(id, description, businessCriticality, active);
        redirect.addFlashAttribute("success", "Service updated.");
        return "redirect:/admin/services";
    }

    // ---------------------------------------------------------------- categories

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("form", new CategoryForm());
        model.addAttribute("categories", catalog.allCategories());
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String createCategory(@Valid @ModelAttribute("form") CategoryForm form, BindingResult errors, Model model,
                                 RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("categories", catalog.allCategories());
            return "admin/categories";
        }
        Category category = catalog.createCategory(form.getName(), form.getDescription(), form.getKeywords());
        redirect.addFlashAttribute("success", "Category " + category.getName() + " added.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(@PathVariable Long id, @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String keywords,
                                 @RequestParam(defaultValue = "false") boolean active, RedirectAttributes redirect) {
        catalog.updateCategory(id, description, keywords, active);
        redirect.addFlashAttribute("success", "Category updated.");
        return "redirect:/admin/categories";
    }

    // ---------------------------------------------------------------- SLA policies

    @GetMapping("/sla")
    public String sla(Model model) {
        model.addAttribute("form", new SlaPolicyForm());
        return slaPage(model);
    }

    @PostMapping("/sla")
    public String saveSla(@Valid @ModelAttribute("form") SlaPolicyForm form, BindingResult errors, Model model,
                          RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return slaPage(model);
        }
        catalog.saveSlaPolicy(form.getServiceId(), form.getPriority(), form.getResponseMinutes(),
                form.getResolutionMinutes());
        redirect.addFlashAttribute("success", "SLA policy saved.");
        return "redirect:/admin/sla";
    }

    @PostMapping("/sla/{id}/delete")
    public String deleteSla(@PathVariable Long id, RedirectAttributes redirect) {
        catalog.deleteSlaPolicy(id);
        redirect.addFlashAttribute("success", "SLA policy removed.");
        return "redirect:/admin/sla";
    }

    private String slaPage(Model model) {
        model.addAttribute("policies", catalog.allSlaPolicies());
        model.addAttribute("services", catalog.allServices());
        model.addAttribute("priorities", Priority.values());
        return "admin/sla";
    }

    // ---------------------------------------------------------------- assignment strategy

    @GetMapping("/strategy")
    public String strategy(Model model) {
        model.addAttribute("strategies", assignment.availableStrategies());
        model.addAttribute("active", assignment.activeStrategy());
        return "admin/strategy";
    }

    @PostMapping("/strategy")
    public String changeStrategy(@RequestParam String name, RedirectAttributes redirect) {
        assignment.changeStrategy(name, me());
        redirect.addFlashAttribute("success", "Assignment strategy changed to '" + name + "'.");
        return "redirect:/admin/strategy";
    }

    // ---------------------------------------------------------------- audit & outbox

    @GetMapping("/audit")
    public String audit(@RequestParam(required = false) String actor, @RequestParam(required = false) String action,
                        @RequestParam(required = false) String incident, Model model) {
        model.addAttribute("entries", audit.recent(actor, action, incident, me()));
        model.addAttribute("actions", IncidentEventType.values());
        model.addAttribute("actor", actor);
        model.addAttribute("action", action);
        model.addAttribute("incident", incident);
        return "admin/audit";
    }

    @GetMapping("/outbox")
    public String outbox(Model model) {
        model.addAttribute("messages", notifications.outbox(me()));
        return "admin/outbox";
    }

    private User me() {
        return currentUser.currentUser();
    }
}
