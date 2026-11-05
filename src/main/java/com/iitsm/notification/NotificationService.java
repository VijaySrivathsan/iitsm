package com.iitsm.notification;

import com.iitsm.domain.notification.ChannelType;
import com.iitsm.domain.notification.Notification;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.User;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The user's in-app inbox (bell icon) and the administrators' view of the simulated email/SMS outbox.
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notifications;
    private final EmailGateway emailGateway;
    private final SmsGateway smsGateway;

    public NotificationService(NotificationRepository notifications, EmailGateway emailGateway,
                               SmsGateway smsGateway) {
        this.notifications = notifications;
        this.emailGateway = emailGateway;
        this.smsGateway = smsGateway;
    }

    @Transactional(readOnly = true)
    public List<Notification> inbox(User user) {
        return notifications.findByRecipientAndChannelOrderByCreatedAtDesc(user, ChannelType.IN_APP);
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notifications.countByRecipientAndChannelAndReadFalse(user, ChannelType.IN_APP);
    }

    public void markRead(Long notificationId, User user) {
        notifications.findById(notificationId).ifPresent(n -> {
            if (!n.getRecipient().equals(user)) {
                throw new UnauthorizedOperationException("This notification belongs to someone else");
            }
            n.markRead();
        });
    }

    public void markAllRead(User user) {
        inbox(user).forEach(Notification::markRead);
    }

    /** Everything the simulated email and SMS gateways have "sent", newest first. Administrators only. */
    @Transactional(readOnly = true)
    public List<OutboxMessage> outbox(User viewer) {
        if (!(viewer instanceof Administrator)) {
            throw new UnauthorizedOperationException("Only administrators can view the outbox");
        }
        return Stream.concat(emailGateway.recentlySent().stream(), smsGateway.recentlySent().stream())
                .sorted(Comparator.comparing(OutboxMessage::sentAt).reversed())
                .toList();
    }
}
