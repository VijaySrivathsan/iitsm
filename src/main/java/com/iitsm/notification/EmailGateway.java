package com.iitsm.notification;

import com.iitsm.exception.ExternalServiceException;

import java.util.List;

/**
 * Adapter pattern - the <i>target</i> interface: what I-ITSM needs from an email provider.
 * The rest of the system depends only on this, so a real provider (SMTP via Spring Mail, SendGrid,
 * ...) can be plugged in by writing one adapter class, without touching notification logic.
 */
public interface EmailGateway {

    /** @throws ExternalServiceException if the provider fails or cannot be reached */
    void send(String toAddress, String subject, String body);

    /** Recently sent messages, newest first (for the admin outbox screen). */
    List<OutboxMessage> recentlySent();
}
