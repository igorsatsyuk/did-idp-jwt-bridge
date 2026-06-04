package lt.satsyuk.didbridge.resource.controller;

import lt.satsyuk.didbridge.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jwt.secret=change-me-in-production-this-must-be-at-least-32-chars"
)
class ResourceApiSecurityIntegrationTest {

    private final JwtService jwtService;

    @Autowired
    ResourceApiSecurityIntegrationTest(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @LocalServerPort
    private int port;

    @Test
    void me_returnsUnauthorized_whenBearerTokenMissing() {
        WebTestClient webTestClient = webTestClient();

        webTestClient.get()
                .uri("/api/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void me_returnsUnauthorized_whenTokenInvalid() {
        WebTestClient webTestClient = webTestClient();

        webTestClient.get()
                .uri("/api/me")
                .header("Authorization", "Bearer not-a-jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void me_returnsClaims_whenTokenValid() {
        WebTestClient webTestClient = webTestClient();
        String token = jwtService.generateToken("did:example:alice", Map.of("role", "user"));

        webTestClient.get()
                .uri("/api/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.did").isEqualTo("did:example:alice")
                .jsonPath("$.claims.role").isEqualTo("user");
    }

    @Test
    void me_returnsUnauthorized_whenTokenSubjectBlank() {
        WebTestClient webTestClient = webTestClient();
        String token = jwtService.generateToken("", Map.of("role", "user"));

        webTestClient.get()
                .uri("/api/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }
}

