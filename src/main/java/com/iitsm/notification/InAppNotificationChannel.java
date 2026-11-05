package com.iitsm.notification;

import com.iitsm.domain.notification.ChannelType;
import com.iitsm.domain.notification.Notification;
import com.iitsm.domain.user.User;
import com.iitsm.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Stores the notification in the database; the web UI shows it under the bell icon until read.
 */
@Component
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationRepository notifications;
    private final Clock clock;

    public InAppNotificationChannel(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Override
    public ChannelType type() {
        return ChannelType.IN_APP;
    }

    @Override
    public void send(User recipient, NotificationMessage message) {
        notifications.save(new Notification(recipient, ChannelType.IN_APP, message.subject(), message.body(),
                message.incidentNumber(), Instant.now(clock)));
    }
}
