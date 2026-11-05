package com.iitsm.web;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.user.User;
import com.iitsm.repository.CategoryRepository;
import com.iitsm.repository.ITServiceRepository;
import com.iitsm.repository.UserRepository;
import com.iitsm.service.IncidentAction;
import com.iitsm.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web layer end-to-end (full application, H2, seeded demo accounts): authentication, role-based URL
 * access, CSRF, form validation and per-incident permissions. Each test is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebSecurityAndFormsTest {

    @Autowired private MockMvc mvc;
    @Autowired private IncidentService incidents;
    @Autowired private UserRepository users;
    @Autowired private ITServiceRepository services;
    @Autowired private CategoryRepository categories;

    private User user(String username) {
        return users.findByUsernameIgnoreCase(username).orElseThrow();
    }

    /** emp1 reports a P1 payment outage; the skill strategy assigns it to tech1. */
    private Incident paymentOutage() {
        ITService payment = services.findByNameIgnoreCase("Payment Gateway").orElseThrow();
        Category application = categories.findByNameIgnoreCase("Application").orElseThrow();
        return incidents.report(user("emp1"), payment.getId(), application.getId(),
                new IncidentReport("Payment down", "Users cannot pay", Impact.HIGH, Urgency.HIGH, 500));
    }

    // ---------------------------------------------------------------- authentication

    @Test
    void anonymousUser_isSentToLogin() throws Exception {
        mvc.perform(get("/employee")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void wrongPassword_isRejected() throws Exception {
        mvc.perform(formLogin().user("emp1").password("not-the-password"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void correctLogin_goesToRoleDashboard() throws Exception {
        mvc.perform(formLogin().user("emp1").password("demo123"))
                .andExpect(redirectedUrl("/dashboard"));
    }

    // ---------------------------------------------------------------- role-based access

    @Test
    @WithUserDetails("emp1")
    void employee_cannotOpenAdminPages() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("tech1")
    void technician_cannotOpenEmployeePages() throws Exception {
        mvc.perform(get("/employee/report")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin")
    void admin_canOpenEveryAdminPage() throws Exception {
        for (String page : new String[]{"/admin", "/admin/incidents", "/admin/users", "/admin/services",
                "/admin/categories", "/admin/sla", "/admin/strategy", "/admin/audit", "/admin/outbox"}) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
    }

    @Test
    @WithUserDetails("emp1")
    void dashboard_redirectsByRole() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(redirectedUrl("/employee"));
    }

    // ---------------------------------------------------------------- forms, validation, CSRF

    @Test
    @WithUserDetails("emp1")
    void reportForm_validIncident_redirectsToItsPage() throws Exception {
        Long serviceId = services.findByNameIgnoreCase("VPN").orElseThrow().getId();
        mvc.perform(post("/employee/report").with(csrf())
                        .param("serviceId", serviceId.toString())
                        .param("title", "VPN keeps disconnecting")
                        .param("description", "Drops every 5 minutes")
                        .param("impact", "MEDIUM").param("urgency", "HIGH").param("affectedUsers", "40"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/incidents/INC*"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithUserDetails("emp1")
    void reportForm_missingFields_showsErrorsAndSavesNothing() throws Exception {
        mvc.perform(post("/employee/report").with(csrf())
                        .param("description", "").param("impact", "HIGH").param("urgency", "HIGH")
                        .param("affectedUsers", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/report"))
                .andExpect(model().attributeHasFieldErrors("form", "serviceId", "description", "affectedUsers"));
    }

    @Test
    @WithUserDetails("emp1")
    void postWithoutCsrfToken_isRejected() throws Exception {
        mvc.perform(post("/employee/report").param("description", "x")).andExpect(status().isForbidden());
    }

    @Test
    void register_newEmployee_thenDuplicateUsernameIsRefused() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "newbie").param("password", "secret1")
                        .param("fullName", "New Person").param("email", "newbie@corp.test"))
                .andExpect(redirectedUrl("/login"));
        assertThat(users.findByUsernameIgnoreCase("newbie")).get()
                .satisfies(u -> assertThat(u.getRole().name()).isEqualTo("EMPLOYEE"));

        mvc.perform(post("/register").with(csrf())
                        .param("username", "emp1").param("password", "secret1").param("fullName", "Copy"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
    }

    // ---------------------------------------------------------------- incident permissions

    @Test
    @WithUserDetails("tech2")
    void technician_cannotResolveSomeoneElsesIncident() throws Exception {
        String number = paymentOutage().getNumber().toString();
        mvc.perform(post("/incidents/" + number + "/resolve").with(csrf()).param("summary", "fixed"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("emp2")
    void employee_cannotViewAnotherEmployeesIncident() throws Exception {
        String number = paymentOutage().getNumber().toString();
        mvc.perform(get("/incidents/" + number)).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("tech1")
    void illegalTransition_comesBackAsErrorBanner() throws Exception {
        String number = paymentOutage().getNumber().toString(); // ASSIGNED to tech1
        mvc.perform(post("/incidents/" + number + "/resolve").with(csrf()).param("summary", "fixed")
                        .header("Referer", "http://localhost/incidents/" + number))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/" + number))
                .andExpect(flash().attribute("error", "Cannot resolve an incident that is ASSIGNED"));
    }

    @Test
    void availableActions_dependOnRoleAndState() {
        Incident incident = paymentOutage(); // ASSIGNED to tech1
        assertThat(incidents.availableActions(incident, user("tech1")))
                .contains(IncidentAction.ACKNOWLEDGE, IncidentAction.ADD_NOTE)
                .doesNotContain(IncidentAction.RESOLVE, IncidentAction.VERIFY);
        assertThat(incidents.availableActions(incident, user("emp1")))
                .contains(IncidentAction.ADD_NOTE, IncidentAction.CHANGE_IMPACT)
                .doesNotContain(IncidentAction.ACKNOWLEDGE, IncidentAction.ASSIGN);
        assertThat(incidents.availableActions(incident, user("admin")))
                .contains(IncidentAction.ASSIGN, IncidentAction.CHANGE_IMPACT)
                .doesNotContain(IncidentAction.ACKNOWLEDGE);
        assertThat(incidents.availableActions(incident, user("emp2"))).isEmpty();
    }

    @Test
    @WithUserDetails("admin")
    void admin_canSwitchAssignmentStrategy() throws Exception {
        mvc.perform(post("/admin/strategy").with(csrf()).param("name", "load"))
                .andExpect(redirectedUrl("/admin/strategy"))
                .andExpect(flash().attributeExists("success"));
    }
}
