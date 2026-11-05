package com.iitsm.notification;

import com.iitsm.exception.ExternalServiceException;

import java.util.List;

/**
 * Adapter pattern - the <i>target</i> interface for SMS providers (Twilio, MSG91, ...).
 */
public interface SmsGateway {

    int MAX_LENGTH = 160;

    /** @throws ExternalServiceException if the provider fails or cannot be reached */
    void send(String phoneNumber, String text);

    /** Recently sent messages, newest first (for the admin outbox screen). */
    List<OutboxMessage> recentlySent();
}
