package com.didbridge.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "change-me-in-production-this-must-be-at-least-32-chars";

    @Test
    void generateToken_andParseToken_roundTripClaims() {
        JwtService service = new JwtService(SECRET, 60);

        String token = service.generateToken("did:example:alice", Map.of("role", "user"));
        Claims claims = service.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("did:example:alice");
        assertThat(claims.get("role", String.class)).isEqualTo("user");
    }

    @Test
    void isValid_returnsTrue_forGeneratedToken() {
        JwtService service = new JwtService(SECRET, 60);
        String token = service.generateToken("did:example:alice", Map.of());

        assertThat(service.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalse_forInvalidToken() {
        JwtService service = new JwtService(SECRET, 60);

        assertThat(service.isValid("not-a-jwt")).isFalse();
    }

    @Test
    void generateToken_usesCustomExpirationAndClaims() {
        JwtService service = new JwtService(SECRET, 60);
        String token = service.generateToken(
                "did:example:alice",
                Map.of("token_type", "refresh"),
                1440
        );

        Claims claims = service.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("did:example:alice");
        assertThat(claims.get("token_type", String.class)).isEqualTo("refresh");
    }

    @Test
    void generateToken_throws_whenCustomExpirationNotPositive() {
        JwtService service = new JwtService(SECRET, 60);
        Map<String, Object> emptyClaims = Map.of();

        assertThatThrownBy(() -> service.generateToken("did:example:alice", emptyClaims, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> service.generateToken("did:example:alice", emptyClaims, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }
}
