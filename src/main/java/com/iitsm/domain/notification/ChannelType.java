package com.iitsm.domain.notification;

/**
 * The ways a notification can be delivered. Each value has a matching NotificationChannel
 * implementation (Milestone 6).
 */
public enum ChannelType {
    IN_APP,
    EMAIL,
    SMS
}
