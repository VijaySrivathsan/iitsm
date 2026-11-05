package com.iitsm.notification;

import com.iitsm.domain.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Simulated email provider (the brief allows simulation): instead of contacting a mail server it
 * writes the message to the log and keeps it in an in-memory outbox that administrators can view.
 * Implements the {@link EmailGateway} interface, so it can be swapped for a real adapter.
 */
@Component
public class LoggingEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailGateway.class);

    private final InMemoryOutbox outbox = new InMemoryOutbox(200);
    private final Clock clock;

    public LoggingEmailGateway(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void send(String toAddress, String subject, String body) {
        Checks.requireText(toAddress, "Email address");
        log.info("[EMAIL] to={} subject=\"{}\"", toAddress, subject);
        outbox.add(new OutboxMessage("EMAIL", toAddress, subject, body, Instant.now(clock)));
    }

    @Override
    public List<OutboxMessage> recentlySent() {
        return outbox.newestFirst();
    }
}
