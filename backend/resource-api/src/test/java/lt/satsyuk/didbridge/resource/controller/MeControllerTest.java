package lt.satsyuk.didbridge.resource.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MeControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void me_returnsDidAndClaimsFromJwt() {
        MeController controller = new MeController();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("did:example:alice")
                .claim("role", "user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        Map<String, Object> result = controller.me(jwt).block();
        Map<String, Object> claims = (Map<String, Object>) result.get("claims");

        assertThat(result).containsEntry("did", "did:example:alice");
        assertThat(claims).containsEntry("role", "user");
    }
}

