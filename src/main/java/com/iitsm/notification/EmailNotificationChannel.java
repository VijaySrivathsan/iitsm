package com.iitsm.notification;

import com.iitsm.domain.notification.ChannelType;
import com.iitsm.domain.notification.Notification;
import com.iitsm.domain.user.User;
import com.iitsm.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Sends the notification through the {@link EmailGateway} and records it as a delivery log entry.
 * Skips users without an email address.
 */
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final EmailGateway gateway;
    private final NotificationRepository notifications;
    private final Clock clock;

    public EmailNotificationChannel(EmailGateway gateway, NotificationRepository notifications, Clock clock) {
        this.gateway = gateway;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Override
    public ChannelType type() {
        return ChannelType.EMAIL;
    }

    @Override
    public boolean accepts(User recipient, NotificationMessage message) {
        return recipient.getEmail() != null;
    }

    @Override
    public void send(User recipient, NotificationMessage message) {
        gateway.send(recipient.getEmail(), message.subject(), message.body());
        notifications.save(new Notification(recipient, ChannelType.EMAIL, message.subject(), message.body(),
                message.incidentNumber(), Instant.now(clock)));
    }
}
