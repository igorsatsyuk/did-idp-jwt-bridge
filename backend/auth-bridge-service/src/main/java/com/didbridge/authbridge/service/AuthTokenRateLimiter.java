package com.didbridge.authbridge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuthTokenRateLimiter {

    private record WindowState(Instant windowEnd, AtomicInteger attempts) {}

    private final long maxAttempts;
    private final long windowSeconds;
    private final long maxTrackedKeys;
    private final Clock clock;
    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();

    @Autowired
    public AuthTokenRateLimiter(
            @Value("${auth.token-rate-limit.max-attempts:10}") long maxAttempts,
            @Value("${auth.token-rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${auth.token-rate-limit.max-tracked-keys:10000}") long maxTrackedKeys
    ) {
        this(maxAttempts, windowSeconds, maxTrackedKeys, Clock.systemUTC());
    }

    AuthTokenRateLimiter(long maxAttempts, long windowSeconds, long maxTrackedKeys, Clock clock) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("auth.token-rate-limit.max-attempts must be > 0");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("auth.token-rate-limit.window-seconds must be > 0");
        }
        if (maxTrackedKeys <= 0) {
            throw new IllegalArgumentException("auth.token-rate-limit.max-tracked-keys must be > 0");
        }
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.maxTrackedKeys = maxTrackedKeys;
        this.clock = clock;
    }

    public synchronized void enforceOrThrow(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Rate limit key must not be blank");
        }
        Instant now = Instant.now(clock);
        WindowState state = windows.get(key);
        if (state != null && state.windowEnd().isAfter(now)) {
            ensureAttemptsWithinLimit(state.attempts().incrementAndGet());
            return;
        }

        ensureDistinctKeyCapacity(key, now);
        WindowState newState = new WindowState(now.plusSeconds(windowSeconds), new AtomicInteger(1));
        state = windows.compute(key, (_, existing) -> {
            if (existing == null || !existing.windowEnd().isAfter(now)) {
                return newState;
            }
            existing.attempts().incrementAndGet();
            return existing;
        });

        ensureAttemptsWithinLimit(state.attempts().get());
    }

    private void ensureDistinctKeyCapacity(String key, Instant now) {
        if (windows.containsKey(key) || windows.size() < maxTrackedKeys) {
            return;
        }
        windows.entrySet().removeIf(entry -> !entry.getValue().windowEnd().isAfter(now));
        if (windows.size() >= maxTrackedKeys) {
            throw new TokenRateLimitExceededException("Too many distinct token request keys");
        }
    }

    private void ensureAttemptsWithinLimit(int attempts) {
        if (attempts > maxAttempts) {
            throw new TokenRateLimitExceededException("Too many token requests");
        }
    }
}
