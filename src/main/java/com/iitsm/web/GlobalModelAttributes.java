package com.iitsm.web;

import com.iitsm.domain.user.User;
import com.iitsm.notification.NotificationService;
import com.iitsm.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds the logged-in user and their unread-notification count (for the bell icon) to every page.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final CurrentUserProvider currentUser;
    private final NotificationService notifications;

    public GlobalModelAttributes(CurrentUserProvider currentUser, NotificationService notifications) {
        this.currentUser = currentUser;
        this.notifications = notifications;
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        return currentUser.currentUserIfLoggedIn().orElse(null);
    }

    @ModelAttribute("unreadCount")
    public long unreadCount() {
        return currentUser.currentUserIfLoggedIn().map(notifications::unreadCount).orElse(0L);
    }
}
