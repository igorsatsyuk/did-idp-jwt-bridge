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
        assertThatThrownBy(() -> new AuthTokenRateLimiter(1, 0, Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window-seconds");
    }
}
