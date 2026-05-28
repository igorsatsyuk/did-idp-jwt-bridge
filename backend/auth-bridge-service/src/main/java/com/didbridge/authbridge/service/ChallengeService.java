package com.didbridge.authbridge.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChallengeService {

    private final Map<String, Instant> issuedChallenges = new ConcurrentHashMap<>();
    private final Clock clock;
    private final long ttlMinutes;
    private final long maxActiveChallenges;

    @Autowired
    public ChallengeService(
            @Value("${auth.challenge-ttl-minutes:5}") long ttlMinutes,
            @Value("${auth.challenge-max-active:10000}") long maxActiveChallenges
    ) {
        this(ttlMinutes, maxActiveChallenges, Clock.systemUTC());
    }

    ChallengeService(long ttlMinutes, Clock clock) {
        this(ttlMinutes, 10000, clock);
    }

    ChallengeService(long ttlMinutes, long maxActiveChallenges, Clock clock) {
        if (ttlMinutes < 0) {
            throw new IllegalArgumentException("auth.challenge-ttl-minutes must be >= 0");
        }
        if (maxActiveChallenges <= 0) {
            throw new IllegalArgumentException("auth.challenge-max-active must be > 0");
        }
        this.ttlMinutes = ttlMinutes;
        this.maxActiveChallenges = maxActiveChallenges;
        this.clock = clock;
    }

    public String issueChallenge() {
        Instant now = Instant.now(clock);
        cleanupExpired(now);
        if (issuedChallenges.size() >= maxActiveChallenges) {
            throw new ChallengeCapacityExceededException("Too many active challenges");
        }

        String challenge = UUID.randomUUID().toString();
        issuedChallenges.put(challenge, now.plusSeconds(ttlMinutes * 60));
        return challenge;
    }

    public void consumeOrThrow(String challenge) {
        assertChallengePresent(challenge);

        Instant now = Instant.now(clock);
        cleanupExpired(now);

        Instant expiresAt = issuedChallenges.remove(challenge);
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new InvalidChallengeException("Challenge is invalid, expired, or already used");
        }
    }

    public void ensureActiveOrThrow(String challenge) {
        assertChallengePresent(challenge);

        Instant now = Instant.now(clock);
        cleanupExpired(now);

        Instant expiresAt = issuedChallenges.get(challenge);
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new InvalidChallengeException("Challenge is invalid, expired, or already used");
        }
    }

    private static void assertChallengePresent(String challenge) {
        if (!StringUtils.hasText(challenge)) {
            throw new InvalidChallengeException("Challenge is missing");
        }
    }

    private void cleanupExpired(Instant now) {
        issuedChallenges.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
