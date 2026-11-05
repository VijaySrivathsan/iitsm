package com.iitsm.web;

import com.iitsm.domain.user.NewUser;
import com.iitsm.exception.InvalidInputException;
import com.iitsm.service.UserService;
import com.iitsm.web.form.RegistrationForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Login page (the login itself is handled by Spring Security) and employee self-registration.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult errors,
                           RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return "register";
        }
        try {
            userService.registerEmployee(new NewUser(form.getUsername(), form.getPassword(), form.getFullName(),
                    form.getEmail(), form.getPhone()), form.getDepartment());
        } catch (InvalidInputException e) {
            errors.reject("register", e.getMessage());
            return "register";
        }
        redirect.addFlashAttribute("success", "Account created - you can now log in.");
        return "redirect:/login";
    }
}
