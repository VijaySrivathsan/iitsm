package com.iitsm.repository;

import com.iitsm.domain.notification.ChannelType;
import com.iitsm.domain.notification.Notification;
import com.iitsm.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for notifications.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientAndChannelOrderByCreatedAtDesc(User recipient, ChannelType channel);

    long countByRecipientAndChannelAndReadFalse(User recipient, ChannelType channel);
}
