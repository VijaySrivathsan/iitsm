package com.iitsm.notification;

import java.time.Instant;

/**
 * A message handed to a (simulated) external gateway, kept so administrators can see what would
 * have been sent by email or SMS.
 */
public record OutboxMessage(String channel, String to, String subject, String body, Instant sentAt) {
}
