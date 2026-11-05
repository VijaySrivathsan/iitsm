package com.iitsm.notification;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A bounded, thread-safe list of the most recent outbox messages. Uses a {@link Deque}: new
 * messages go on the front and the oldest drop off the back once the limit is reached.
 * Shared by the simulated gateways (composition, not inheritance).
 */
public class InMemoryOutbox {

    private final int capacity;
    private final Deque<OutboxMessage> messages = new ArrayDeque<>();

    public InMemoryOutbox(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void add(OutboxMessage message) {
        messages.addFirst(message);
        while (messages.size() > capacity) {
            messages.removeLast();
        }
    }

    public synchronized List<OutboxMessage> newestFirst() {
        return List.copyOf(messages);
    }
}
