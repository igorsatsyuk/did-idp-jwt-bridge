package com.didbridge.authbridge.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

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
}
