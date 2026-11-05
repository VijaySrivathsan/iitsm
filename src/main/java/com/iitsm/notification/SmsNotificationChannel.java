package com.iitsm.notification;

import com.iitsm.domain.notification.ChannelType;
import com.iitsm.domain.notification.Notification;
import com.iitsm.domain.user.User;
import com.iitsm.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Sends a short text through the {@link SmsGateway} - only for urgent messages (major incidents,
 * SLA problems, escalations) and only to users with a phone number.
 */
@Component
public class SmsNotificationChannel implements NotificationChannel {

    private final SmsGateway gateway;
    private final NotificationRepository notifications;
    private final Clock clock;

    public SmsNotificationChannel(SmsGateway gateway, NotificationRepository notifications, Clock clock) {
        this.gateway = gateway;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Override
    public ChannelType type() {
        return ChannelType.SMS;
    }

    @Override
    public boolean accepts(User recipient, NotificationMessage message) {
        return message.urgent() && recipient.getPhone() != null;
    }

    @Override
    public void send(User recipient, NotificationMessage message) {
        gateway.send(recipient.getPhone(), message.shortText());
        notifications.save(new Notification(recipient, ChannelType.SMS, message.subject(), message.shortText(),
                message.incidentNumber(), Instant.now(clock)));
    }
}
