package com.iitsm.web;

import com.iitsm.domain.user.User;
import com.iitsm.security.CurrentUserProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

/**
 * Landing page, the role-based "/dashboard" redirect, and the access-denied page.
 */
@Controller
public class HomeController {

    private final CurrentUserProvider currentUser;

    public HomeController(CurrentUserProvider currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/")
    public String home() {
        return currentUser.currentUserIfLoggedIn().isPresent() ? "redirect:/dashboard" : "index";
    }

    /** Each role has its own home page (different GUI per user type, as the brief asks). */
    @GetMapping("/dashboard")
    public String dashboard() {
        Optional<User> user = currentUser.currentUserIfLoggedIn();
        if (user.isEmpty()) {
            return "redirect:/login";
        }
        return switch (user.get().getRole()) {
            case EMPLOYEE -> "redirect:/employee";
            case TECHNICIAN -> "redirect:/tech";
            case ADMIN -> "redirect:/admin";
        };
    }

    @GetMapping("/error/403")
    public String accessDenied(Model model) {
        model.addAttribute("message", "You do not have permission to open that page.");
        return "error/403";
    }
}
