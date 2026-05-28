package com.didbridge.authbridge.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
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

    @Test
    void consumeOrThrow_acceptsFreshChallengeExactlyOnce() {
        ChallengeService service = new ChallengeService(5, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        String challenge = service.issueChallenge();
        service.ensureActiveOrThrow(challenge);

        service.consumeOrThrow(challenge);

        assertThatThrownBy(() -> service.consumeOrThrow(challenge))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("invalid, expired, or already used");
    }

    @Test
    void consumeOrThrow_rejectsExpiredChallenge() {
        Clock fixed = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        ChallengeService service = new ChallengeService(0, fixed);
        String challenge = service.issueChallenge();

        assertThatThrownBy(() -> service.ensureActiveOrThrow(challenge))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("invalid, expired, or already used");
        assertThatThrownBy(() -> service.consumeOrThrow(challenge))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("invalid, expired, or already used");
    }

    @Test
    void consumeOrThrow_rejectsBlankChallenge() {
        ChallengeService service = new ChallengeService(5, Clock.systemUTC());

        assertThatThrownBy(() -> service.consumeOrThrow(" "))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void issueChallenge_returnsUuidString() {
        ChallengeService service = new ChallengeService(5, Clock.systemUTC());

        String challenge = service.issueChallenge();

        assertThat(challenge).isNotBlank();
    }

    @Test
    void issueChallenge_rejectsWhenCapacityExceeded() {
        Clock fixed = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        ChallengeService service = new ChallengeService(5, 1, fixed);
        service.issueChallenge();

        assertThatThrownBy(service::issueChallenge)
                .isInstanceOf(ChallengeCapacityExceededException.class)
                .hasMessageContaining("Too many active challenges");
    }

    @Test
    void constructor_rejectsInvalidMaxActiveConfig() {
        assertThatThrownBy(() -> new ChallengeService(5, 0, Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("challenge-max-active");
    }

    @Test
    void issueChallenge_enforcesCapacityUnderConcurrentCalls() throws Exception {
        Clock fixed = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        ChallengeService service = new ChallengeService(5, 1, fixed);
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
}
