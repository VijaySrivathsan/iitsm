package com.iitsm.domain.notification;

import com.iitsm.domain.BaseEntity;
import com.iitsm.domain.Checks;
import com.iitsm.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A message sent to a user. In-app notifications are shown under the bell icon; email and SMS ones
 * are kept as a delivery log.
 */
@Entity
@Table(name = "notification")
public class Notification extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChannelType channel;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(length = 20)
    private String incidentNumber;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(name = "is_read")
    private boolean read;

    protected Notification() {
    }

    public Notification(User recipient, ChannelType channel, String subject, String body, String incidentNumber,
                        Instant createdAt) {
        this.recipient = Checks.requireNonNull(recipient, "Recipient");
        this.channel = Checks.requireNonNull(channel, "Channel");
        this.subject = Checks.requireText(subject, "Subject");
        this.body = Checks.requireText(body, "Body");
        this.incidentNumber = Checks.optionalText(incidentNumber);
        this.createdAt = Checks.requireNonNull(createdAt, "Creation time");
    }

    public void markRead() {
        this.read = true;
    }

    public User getRecipient() {
        return recipient;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }
}
