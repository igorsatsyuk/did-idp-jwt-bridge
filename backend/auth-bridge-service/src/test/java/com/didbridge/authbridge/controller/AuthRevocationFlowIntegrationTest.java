package com.didbridge.authbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jwt.secret=change-me-in-production-this-must-be-at-least-32-chars"
)
class AuthRevocationFlowIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ConcurrentHashMap<String, DidDocumentPayload> DID_STORE = new ConcurrentHashMap<>();

    private static HttpServer identityStubServer;

    @LocalServerPort
    private int authBridgePort;

    @DynamicPropertySource
    static void identityServiceUrl(DynamicPropertyRegistry registry) {
        ensureIdentityStubStarted();
        registry.add("services.identity-url", () -> "http://localhost:" + identityStubServer.getAddress().getPort());
    }

    @AfterAll
    static void stopStub() {
        if (identityStubServer != null) {
            identityStubServer.stop(0);
        }
    }

    @Test
    void registerThenRevokeThenAuth_returnsUnauthorized() throws Exception {
        String did = "did:ethr:0x1111111111111111111111111111111111111111";
        WebTestClient webTestClient = webTestClient();
        registerDid(did, "0x04abc");

        String challenge = webTestClient.get()
                .uri("http://localhost:" + authBridgePort + "/auth/challenge")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(challenge).isNotBlank();

        revokeDid(did);

        webTestClient.post()
                .uri("http://localhost:" + authBridgePort + "/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "did", did,
                        "challenge", challenge,
                        "signature", "0xdeadbeef"
                ))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private static synchronized void ensureIdentityStubStarted() {
        if (identityStubServer != null) {
            return;
        }

        try {
            identityStubServer = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create identity stub server", e);
        }

        identityStubServer.createContext("/did/register", AuthRevocationFlowIntegrationTest::handleRegisterDid);
        identityStubServer.createContext("/did", AuthRevocationFlowIntegrationTest::handleDidRoutes);
        identityStubServer.start();
    }

    private static void handleRegisterDid(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeResponse(exchange, 405, "");
            return;
        }

        RegisterDidPayload payload;
        try (InputStream body = exchange.getRequestBody()) {
            payload = OBJECT_MAPPER.readValue(body, RegisterDidPayload.class);
        }

        if (payload == null || payload.did == null || payload.did.isBlank() || payload.publicKey == null || payload.publicKey.isBlank()) {
            writeResponse(exchange, 400, "{\"message\":\"Invalid request payload\"}");
            return;
        }

        Instant now = Instant.now();
        DidDocumentPayload doc = new DidDocumentPayload(payload.did, payload.publicKey, "ACTIVE", now.toString(), now.toString());
        DID_STORE.put(payload.did, doc);

        writeJson(exchange, 201, doc);
    }

    private static void handleDidRoutes(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith("/did/")) {
            writeResponse(exchange, 404, "");
            return;
        }

        String suffix = path.substring("/did/".length());
        if (suffix.isBlank()) {
            writeResponse(exchange, 404, "");
            return;
        }

        if (suffix.endsWith("/revoke")) {
            String did = suffix.substring(0, suffix.length() - "/revoke".length());
            handleRevokeDid(exchange, decodeDid(did));
            return;
        }

        handleGetDid(exchange, decodeDid(suffix));
    }

    private static void handleGetDid(HttpExchange exchange, String did) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeResponse(exchange, 405, "");
            return;
        }

        DidDocumentPayload doc = DID_STORE.get(did);
        if (doc == null) {
            writeResponse(exchange, 404, "{\"message\":\"DID not found\"}");
            return;
        }

        writeJson(exchange, 200, doc);
    }

    private static void handleRevokeDid(HttpExchange exchange, String did) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeResponse(exchange, 405, "");
            return;
        }

        DidDocumentPayload existing = DID_STORE.get(did);
        if (existing == null) {
            writeResponse(exchange, 404, "{\"message\":\"DID not found\"}");
            return;
        }

        DidDocumentPayload revoked = new DidDocumentPayload(
                existing.did,
                existing.publicKey,
                "REVOKED",
                existing.createdAt,
                Instant.now().toString()
        );
        DID_STORE.put(did, revoked);
        writeResponse(exchange, 204, "");
    }

    private void registerDid(String did, String publicKey) throws Exception {
        String baseUrl = "http://localhost:" + identityStubServer.getAddress().getPort();
        String body = OBJECT_MAPPER.writeValueAsString(Map.of("did", did, "publicKey", publicKey));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/did/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
    }

    private void revokeDid(String did) throws Exception {
        String baseUrl = "http://localhost:" + identityStubServer.getAddress().getPort();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/did/" + did + "/revoke"))
                .DELETE()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(204);
    }

    private static String decodeDid(String rawDid) {
        return URI.create("http://localhost/" + rawDid).getPath().substring(1);
    }

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + authBridgePort)
                .build();
    }

    private static void writeJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] body = OBJECT_MAPPER.writeValueAsBytes(payload);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static void writeResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = Objects.requireNonNullElse(body, "").getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static final class RegisterDidPayload {
        public String did;
        public String publicKey;
    }

    private static final class DidDocumentPayload {
        public String did;
        public String publicKey;
        public String status;
        public String createdAt;
        public String updatedAt;

        private DidDocumentPayload() {
        }

        private DidDocumentPayload(String did, String publicKey, String status, String createdAt, String updatedAt) {
            this.did = did;
            this.publicKey = publicKey;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }
}
