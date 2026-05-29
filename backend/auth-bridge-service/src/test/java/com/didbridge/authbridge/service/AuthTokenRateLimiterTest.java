package com.didbridge.authbridge.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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

    @Test
    void constructor_throwsWhenMaxTrackedKeysInvalid() {
        Clock clock = Clock.systemUTC();
        assertThatThrownBy(() -> new AuthTokenRateLimiter(1, 60, 0, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-tracked-keys");
    }

    @Test
    void enforceOrThrow_allowsNewKeyAfterExpiredEntriesAreCleaned() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AuthTokenRateLimiter limiter = new AuthTokenRateLimiter(2, 60, 2, clock);

        limiter.enforceOrThrow("did:example:alice");
        limiter.enforceOrThrow("did:example:bob");

        clock.set(Instant.parse("2026-01-01T00:02:00Z"));
        limiter.enforceOrThrow("did:example:charlie");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
