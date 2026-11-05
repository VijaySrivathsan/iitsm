package com.iitsm.notification;

import com.iitsm.domain.notification.ChannelType;
import com.iitsm.domain.user.User;

/**
 * Strategy pattern: one way of delivering a notification (in-app, email, SMS, ...). The listener
 * picks channels per recipient and calls {@link #send} on each without knowing how it works.
 * Adding a channel (e.g. push notifications) = one new implementation + one {@link ChannelType}
 * value; no existing class changes.
 */
public interface NotificationChannel {

    ChannelType type();

    /** Whether this channel wants to deliver this message to this user (e.g. SMS only when urgent). */
    default boolean accepts(User recipient, NotificationMessage message) {
        return true;
    }

    /** @throws com.iitsm.exception.ExternalServiceException if delivery fails */
    void send(User recipient, NotificationMessage message);
}
