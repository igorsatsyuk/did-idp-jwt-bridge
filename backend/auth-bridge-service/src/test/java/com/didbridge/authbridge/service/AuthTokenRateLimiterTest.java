package com.didbridge.authbridge.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthTokenRateLimiterTest {

    @Test
    void enforceOrThrow_throwsWhenLimitExceededWithinWindow() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        AuthTokenRateLimiter limiter = new AuthTokenRateLimiter(
                2,
                60,
                100,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        limiter.enforceOrThrow("did:example:alice");
        limiter.enforceOrThrow("did:example:alice");

        assertThatThrownBy(() -> limiter.enforceOrThrow("did:example:alice"))
                .isInstanceOf(TokenRateLimitExceededException.class)
                .hasMessageContaining("Too many token requests");
    }

    @Test
    void constructor_throwsWhenWindowSecondsInvalid() {
        Clock clock = Clock.systemUTC();
        assertThatThrownBy(() -> new AuthTokenRateLimiter(1, 0, 10, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window-seconds");
    }

    @Test
    void enforceOrThrow_throwsWhenTrackedKeysLimitExceeded() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        AuthTokenRateLimiter limiter = new AuthTokenRateLimiter(
                2,
                60,
                2,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        limiter.enforceOrThrow("did:example:alice");
        limiter.enforceOrThrow("did:example:bob");

        assertThatThrownBy(() -> limiter.enforceOrThrow("did:example:charlie"))
                .isInstanceOf(TokenRateLimitExceededException.class)
                .hasMessageContaining("distinct token request keys");
    }
}
