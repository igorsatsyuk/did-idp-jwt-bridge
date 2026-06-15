package lt.satsyuk.didbridge.authbridge.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthTokenRateLimiterTest {
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void enforceOrThrow_throwsWhenLimitExceededWithinWindow() {
        AuthTokenRateLimiter limiter = new AuthTokenRateLimiter(
                2,
                60,
                100,
                FIXED_CLOCK
        );

        limiter.enforceOrThrow("did:example:alice");
        limiter.enforceOrThrow("did:example:alice");

        assertThatThrownBy(() -> limiter.enforceOrThrow("did:example:alice"))
                .isInstanceOf(TokenRateLimitExceededException.class)
                .hasMessageContaining("Too many token requests");
    }

    @Test
    void constructor_throwsWhenWindowSecondsInvalid() {
        assertThatThrownBy(() -> new AuthTokenRateLimiter(1, 0, 10, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window-seconds");
    }

    @Test
    void enforceOrThrow_throwsWhenTrackedKeysLimitExceeded() {
        AuthTokenRateLimiter limiter = new AuthTokenRateLimiter(
                2,
                60,
                2,
                FIXED_CLOCK
        );

        limiter.enforceOrThrow("did:example:alice");
        limiter.enforceOrThrow("did:example:bob");

        assertThatThrownBy(() -> limiter.enforceOrThrow("did:example:charlie"))
                .isInstanceOf(TokenRateLimitExceededException.class)
                .hasMessageContaining("distinct token request keys");
    }

    @Test
    void constructor_throwsWhenMaxTrackedKeysInvalid() {
        assertThatThrownBy(() -> new AuthTokenRateLimiter(1, 60, 0, FIXED_CLOCK))
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
        assertThatCode(() -> limiter.enforceOrThrow("did:example:charlie"))
                .doesNotThrowAnyException();
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

