package com.didbridge.resource.config;

import com.didbridge.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceReactiveJwtDecoderTest {

    private static final String SECRET = "change-me-in-production-this-must-be-at-least-32-chars";

    @Test
    void decode_returnsJwtWithClaims_forValidToken() {
        JwtService jwtService = new JwtService(SECRET, 60);
        JwtServiceReactiveJwtDecoder decoder = new JwtServiceReactiveJwtDecoder(jwtService);
        String token = jwtService.generateToken("did:example:alice", Map.of("role", "user"));

        Jwt jwt = decoder.decode(token).block();

        assertThat(jwt.getSubject()).isEqualTo("did:example:alice");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("user");
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
    }

    @Test
    void decode_throwsJwtException_forInvalidToken() {
        JwtService jwtService = new JwtService(SECRET, 60);
        JwtServiceReactiveJwtDecoder decoder = new JwtServiceReactiveJwtDecoder(jwtService);

        assertThatThrownBy(() -> decoder.decode("not-a-jwt").block())
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid JWT token");
    }
}
