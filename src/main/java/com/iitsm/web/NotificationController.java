package com.iitsm.web;

import com.iitsm.notification.NotificationService;
import com.iitsm.security.CurrentUserProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * The in-app notification inbox (behind the bell icon).
 */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notifications;
    private final CurrentUserProvider currentUser;

    public NotificationController(NotificationService notifications, CurrentUserProvider currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String inbox(Model model) {
        model.addAttribute("notifications", notifications.inbox(currentUser.currentUser()));
        return "notifications";
    }

    /** Marks one notification read and opens its incident (or the inbox). */
    @PostMapping("/{id}/open")
    public String open(@PathVariable Long id, @org.springframework.web.bind.annotation.RequestParam(required = false)
                       String incident) {
        notifications.markRead(id, currentUser.currentUser());
        return incident == null || incident.isBlank() ? "redirect:/notifications" : "redirect:/incidents/" + incident;
    }

    @PostMapping("/read-all")
    public String readAll() {
        notifications.markAllRead(currentUser.currentUser());
        return "redirect:/notifications";
    }
}
