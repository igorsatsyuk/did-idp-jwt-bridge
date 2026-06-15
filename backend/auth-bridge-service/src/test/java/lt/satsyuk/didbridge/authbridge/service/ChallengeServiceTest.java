package lt.satsyuk.didbridge.authbridge.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChallengeServiceTest {
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void consumeOrThrow_acceptsFreshChallengeExactlyOnce() {
        ChallengeService service = new ChallengeService(5, FIXED_CLOCK);
        String challenge = service.issueChallenge();
        service.ensureActiveOrThrow(challenge);

        service.consumeOrThrow(challenge);

        assertThatThrownBy(() -> service.consumeOrThrow(challenge))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("invalid, expired, or already used");
    }

    @Test
    void consumeOrThrow_rejectsExpiredChallenge() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        ChallengeService service = new ChallengeService(1, clock);
        String challenge = service.issueChallenge();
        clock.advance(Duration.ofMinutes(1));

        assertThatThrownBy(() -> service.ensureActiveOrThrow(challenge))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("invalid, expired, or already used");
        assertThatThrownBy(() -> service.consumeOrThrow(challenge))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("invalid, expired, or already used");
    }

    @Test
    void consumeOrThrow_rejectsBlankChallenge() {
        ChallengeService service = new ChallengeService(5, FIXED_CLOCK);

        assertThatThrownBy(() -> service.consumeOrThrow(" "))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void issueChallenge_returnsUuidString() {
        ChallengeService service = new ChallengeService(5, FIXED_CLOCK);

        String challenge = service.issueChallenge();

        assertThat(challenge).isNotBlank();
    }

    @Test
    void issueChallenge_rejectsWhenCapacityExceeded() {
        ChallengeService service = new ChallengeService(5, 1, FIXED_CLOCK);
        service.issueChallenge();

        assertThatThrownBy(service::issueChallenge)
                .isInstanceOf(ChallengeCapacityExceededException.class)
                .hasMessageContaining("Too many active challenges");
    }

    @Test
    void constructor_rejectsInvalidMaxActiveConfig() {
        assertThatThrownBy(() -> new ChallengeService(5, 0, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("challenge-max-active");
    }

    @Test
    void constructor_rejectsNonPositiveTtlConfig() {
        assertThatThrownBy(() -> new ChallengeService(0, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("challenge-ttl-minutes");
    }

    @Test
    void issueChallenge_enforcesCapacityUnderConcurrentCalls() throws Exception {
        ChallengeService service = new ChallengeService(5, 1, FIXED_CLOCK);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Object> issue = () -> {
                start.await();
                try {
                    return service.issueChallenge();
                } catch (RuntimeException ex) {
                    return ex;
                }
            };
            Future<Object> first = executor.submit(issue);
            Future<Object> second = executor.submit(issue);
            start.countDown();

            Object result1 = first.get();
            Object result2 = second.get();

            assertThat(Stream.of(result1, result2).filter(String.class::isInstance).count()).isEqualTo(1);
            assertThat(Stream.of(result1, result2)
                    .filter(ChallengeCapacityExceededException.class::isInstance)
                    .count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void ensureActiveOrThrow_rejectsChallengeIssuedByDifferentInstance() {
        ChallengeService issuer = new ChallengeService(5, 10, "instance-a", FIXED_CLOCK);
        ChallengeService validator = new ChallengeService(5, 10, "instance-b", FIXED_CLOCK);
        String challenge = issuer.issueChallenge();

        assertThatThrownBy(() -> validator.ensureActiveOrThrow(challenge))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("different auth-bridge instance");
    }

    @Test
    void constructor_rejectsBlankInstanceId() {
        assertThatThrownBy(() -> new ChallengeService(5, 10, " ", FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("auth.instance-id");
    }

    @Test
    void constructor_rejectsInstanceIdContainingSeparator() {
        assertThatThrownBy(() -> new ChallengeService(5, 10, "node:a", FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain ':'");
    }

    private static final class MutableClock extends Clock {
        private Instant current;
        private final ZoneId zone;

        private MutableClock(Instant current, ZoneId zone) {
            this.current = current;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(current, zone);
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}

