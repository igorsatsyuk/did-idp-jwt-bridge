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
    private final Clock clock;
    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();

    @Autowired
    public AuthTokenRateLimiter(
            @Value("${auth.token-rate-limit.max-attempts:10}") long maxAttempts,
            @Value("${auth.token-rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this(maxAttempts, windowSeconds, Clock.systemUTC());
    }

    AuthTokenRateLimiter(long maxAttempts, long windowSeconds, Clock clock) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("auth.token-rate-limit.max-attempts must be > 0");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("auth.token-rate-limit.window-seconds must be > 0");
        }
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    public void enforceOrThrow(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Rate limit key must not be blank");
        }
        Instant now = Instant.now(clock);
        WindowState state = windows.compute(key, (_, existing) -> {
            if (existing == null || !existing.windowEnd().isAfter(now)) {
                return new WindowState(now.plusSeconds(windowSeconds), new AtomicInteger(1));
            }
            existing.attempts().incrementAndGet();
            return existing;
        });

        if (state.attempts().get() > maxAttempts) {
            throw new TokenRateLimitExceededException("Too many token requests");
        }
    }
}
