package com.didbridge.resource.config;

import com.didbridge.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class JwtServiceReactiveJwtDecoder implements ReactiveJwtDecoder {

    private static final String ISSUED_AT_CLAIM = "iat";
    private static final String EXPIRES_AT_CLAIM = "exp";
    private final JwtService jwtService;

    public JwtServiceReactiveJwtDecoder(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Jwt> decode(String token) {
        return Mono.fromCallable(() -> {
            try {
                Jws<Claims> signedClaims = jwtService.parseSignedClaims(token);
                Claims claims = signedClaims.getPayload();
                Map<String, Object> headers = new HashMap<>(signedClaims.getHeader());
                Map<String, Object> jwtClaims = new HashMap<>(claims);

                Instant issuedAt = claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : null;
                Instant expiresAt = claims.getExpiration() != null ? claims.getExpiration().toInstant() : null;
                if (issuedAt != null) {
                    jwtClaims.put(ISSUED_AT_CLAIM, issuedAt);
                }
                if (expiresAt != null) {
                    jwtClaims.put(EXPIRES_AT_CLAIM, expiresAt);
                }

                return new Jwt(token, issuedAt, expiresAt, headers, jwtClaims);
            } catch (Exception ex) {
                throw new BadJwtException("Invalid JWT token", ex);
            }
        });
    }
}
