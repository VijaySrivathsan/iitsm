package com.iitsm.notification;

import com.iitsm.domain.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Simulated SMS provider: logs the text (cut to 160 characters, like a real SMS) and keeps it in an
 * in-memory outbox. Swappable for a real provider adapter behind {@link SmsGateway}.
 */
@Component
public class LoggingSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsGateway.class);

    private final InMemoryOutbox outbox = new InMemoryOutbox(200);
    private final Clock clock;

    public LoggingSmsGateway(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void send(String phoneNumber, String text) {
        Checks.requireText(phoneNumber, "Phone number");
        String sms = text.length() <= MAX_LENGTH ? text : text.substring(0, MAX_LENGTH - 3) + "...";
        log.info("[SMS] to={} text=\"{}\"", phoneNumber, sms);
        outbox.add(new OutboxMessage("SMS", phoneNumber, null, sms, Instant.now(clock)));
    }

    @Override
    public List<OutboxMessage> recentlySent() {
        return outbox.newestFirst();
    }
}
