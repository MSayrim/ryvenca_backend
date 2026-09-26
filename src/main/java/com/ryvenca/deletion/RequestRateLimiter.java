package com.ryvenca.deletion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** Small in-memory sliding-window limiter for public, unauthenticated endpoints. */
@Component
public class RequestRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final Clock clock;

    public RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** @return false when {@code key} exceeded the limit. */
    public boolean allow(String key) {
        Instant now = clock.instant();
        Deque<Instant> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(now.minus(WINDOW))) {
                window.pollFirst();
            }
            if (window.size() >= MAX_REQUESTS) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }
}
