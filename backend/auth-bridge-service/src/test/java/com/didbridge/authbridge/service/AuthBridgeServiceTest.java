package com.didbridge.authbridge.service;

import com.didbridge.authbridge.dto.AuthRequest;
import com.didbridge.authbridge.dto.AuthResponse;
import com.didbridge.model.DidDocument;
import com.didbridge.model.DidStatus;
import com.didbridge.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthBridgeServiceTest {
    private static final String CLIENT_ADDRESS = "127.0.0.1";


    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @Mock
    private JwtService jwtService;
    @Mock
    private SignatureVerifier signatureVerifier;
    @Mock
    private ChallengeService challengeService;
    @Mock
    private AuthTokenRateLimiter authTokenRateLimiter;

    private AuthBridgeService service;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl("http://identity-service:8081")).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        lenient().when(webClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        service = new AuthBridgeService(
                webClientBuilder,
                jwtService,
                signatureVerifier,
                challengeService,
                authTokenRateLimiter,
                60,
                10080,
                "http://identity-service:8081");
    }

    @Test
    void authenticate_returnsToken_whenDidActiveAndSignatureValid() {
        AuthRequest request = new AuthRequest("did:example:alice", "challenge-1", "0xsignature");
        DidDocument doc = new DidDocument(
                request.did(), "0xpublic", DidStatus.ACTIVE, Instant.now(), Instant.now());

        when(requestHeadersUriSpec.uri("/did/{did}", request.did()))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(responseSpec.bodyToMono(DidDocument.class)).thenReturn(Mono.just(doc));
        when(signatureVerifier.verify(request.challenge(), request.signature(), doc.publicKey())).thenReturn(true);
        when(jwtService.generateToken(request.did(), Map.of("did", request.did()), 60)).thenReturn("jwt-token");
        when(jwtService.generateToken(
                request.did(),
                Map.of("did", request.did(), "token_type", "refresh"),
                10080
        )).thenReturn("refresh-token");

        AuthResponse response = service.authenticate(request, CLIENT_ADDRESS).block();

        assertThat(response).isEqualTo(new AuthResponse("jwt-token", "Bearer", 3600, "refresh-token", 604800));
        verify(authTokenRateLimiter).enforceOrThrow(CLIENT_ADDRESS + "|" + request.did());
        verify(challengeService).ensureActiveOrThrow(request.challenge());
        verify(challengeService).consumeOrThrow(request.challenge());
    }

    @Test
    void authenticate_returnsUnauthorized_whenDidRevoked() {
        AuthRequest request = new AuthRequest("did:example:alice", "challenge-1", "0xsignature");
        DidDocument doc = new DidDocument(
                request.did(), "0xpublic", DidStatus.REVOKED, Instant.now(), Instant.now());

        when(requestHeadersUriSpec.uri("/did/{did}", request.did()))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(responseSpec.bodyToMono(DidDocument.class)).thenReturn(Mono.just(doc));

        Mono<AuthResponse> authMono = service.authenticate(request, CLIENT_ADDRESS);
        assertThatThrownBy(authMono::block)
                .isInstanceOf(DidRevokedException.class)
                .hasMessageContaining("revoked");
        verify(authTokenRateLimiter).enforceOrThrow(CLIENT_ADDRESS + "|" + request.did());
        verify(challengeService).ensureActiveOrThrow(request.challenge());
        verify(challengeService, never()).consumeOrThrow(request.challenge());
    }

    @Test
    void authenticate_returnsError_whenSignatureInvalid() {
        AuthRequest request = new AuthRequest("did:example:alice", "challenge-1", "0xsignature");
        DidDocument doc = new DidDocument(
                request.did(), "0xpublic", DidStatus.ACTIVE, Instant.now(), Instant.now());

        when(requestHeadersUriSpec.uri("/did/{did}", request.did()))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(responseSpec.bodyToMono(DidDocument.class)).thenReturn(Mono.just(doc));
        when(signatureVerifier.verify(request.challenge(), request.signature(), doc.publicKey())).thenReturn(false);

        Mono<AuthResponse> authMono = service.authenticate(request, CLIENT_ADDRESS);
        assertThatThrownBy(authMono::block)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid signature");
        verify(authTokenRateLimiter).enforceOrThrow(CLIENT_ADDRESS + "|" + request.did());
        verify(challengeService).ensureActiveOrThrow(request.challenge());
        verify(challengeService, never()).consumeOrThrow(request.challenge());
    }

    @Test
    void authenticate_returnsUnauthorized_whenChallengeInvalidOrReplayed() {
        AuthRequest request = new AuthRequest("did:example:alice", "replayed", "0xsignature");
        doThrow(new InvalidChallengeException("Challenge is invalid, expired, or already used"))
                .when(challengeService).ensureActiveOrThrow(request.challenge());

        Mono<AuthResponse> authMono = service.authenticate(request, CLIENT_ADDRESS);
        assertThatThrownBy(authMono::block)
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("invalid, expired, or already used");
        verify(authTokenRateLimiter).enforceOrThrow(CLIENT_ADDRESS + "|" + request.did());
    }

    @Test
    void generateChallenge_returnsUuidString() {
        String issued = UUID.randomUUID().toString();
        when(challengeService.issueChallenge()).thenReturn(issued);
        String challenge = service.generateChallenge().block();

        assertThat(challenge).isEqualTo(issued);
    }

    @Test
    void authenticate_returnsTooManyRequests_whenRateLimitExceeded() {
        AuthRequest request = new AuthRequest("did:example:alice", "challenge-1", "0xsignature");
        doThrow(new TokenRateLimitExceededException("Too many token requests"))
                .when(authTokenRateLimiter).enforceOrThrow(CLIENT_ADDRESS + "|" + request.did());

        Mono<AuthResponse> authMono = service.authenticate(request, CLIENT_ADDRESS);
        assertThatThrownBy(authMono::block)
                .isInstanceOf(TokenRateLimitExceededException.class)
                .hasMessageContaining("Too many token requests");
    }

    @Test
    void refreshAccessToken_returnsNewTokens_whenRefreshTokenValid() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("did:example:alice");
        when(claims.get("token_type", String.class)).thenReturn("refresh");
        when(jwtService.parseToken("refresh-token")).thenReturn(claims);

        DidDocument doc = new DidDocument(
                "did:example:alice", "0xpublic", DidStatus.ACTIVE, Instant.now(), Instant.now());
        when(requestHeadersUriSpec.uri("/did/{did}", "did:example:alice"))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(responseSpec.bodyToMono(DidDocument.class)).thenReturn(Mono.just(doc));
        when(jwtService.generateToken("did:example:alice", Map.of("did", "did:example:alice"), 60))
                .thenReturn("new-access-token");
        when(jwtService.generateToken(
                "did:example:alice",
                Map.of("did", "did:example:alice", "token_type", "refresh"),
                10080
        )).thenReturn("new-refresh-token");

        AuthResponse response = service.refreshAccessToken("refresh-token").block();

        assertThat(response).isEqualTo(
                new AuthResponse("new-access-token", "Bearer", 3600, "new-refresh-token", 604800));
        verify(jwtService, times(1)).parseToken("refresh-token");
    }

    @Test
    void refreshAccessToken_throwsInvalidRefreshTokenException_whenTokenTypeInvalid() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("did:example:alice");
        when(claims.get("token_type", String.class)).thenReturn("access");
        when(jwtService.parseToken("not-refresh")).thenReturn(claims);

        Mono<AuthResponse> refreshMono = service.refreshAccessToken("not-refresh");
        assertThatThrownBy(refreshMono::block)
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("not a refresh token");
    }

    @Test
    void refreshAccessToken_throwsInvalidRefreshTokenException_whenIdentityServiceErrors() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("did:example:alice");
        when(claims.get("token_type", String.class)).thenReturn("refresh");
        when(jwtService.parseToken("refresh-token")).thenReturn(claims);
        when(requestHeadersUriSpec.uri("/did/{did}", "did:example:alice"))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(responseSpec.bodyToMono(DidDocument.class)).thenReturn(Mono.error(
                WebClientResponseException.create(
                        404,
                        "Not Found",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                )));

        Mono<AuthResponse> refreshMono = service.refreshAccessToken("refresh-token");
        assertThatThrownBy(refreshMono::block)
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Refresh token is invalid");
    }

    @Test
    void refreshAccessToken_throwsIdentityServiceUnavailableException_whenIdentityServiceFailsWith5xx() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("did:example:alice");
        when(claims.get("token_type", String.class)).thenReturn("refresh");
        when(jwtService.parseToken("refresh-token")).thenReturn(claims);
        when(requestHeadersUriSpec.uri("/did/{did}", "did:example:alice"))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(responseSpec.bodyToMono(DidDocument.class)).thenReturn(Mono.error(
                WebClientResponseException.create(
                        503,
                        "Service Unavailable",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                )));

        Mono<AuthResponse> refreshMono = service.refreshAccessToken("refresh-token");
        assertThatThrownBy(refreshMono::block)
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining("Identity service request failed");
    }

    @Test
    void refreshAccessToken_throwsIdentityServiceUnavailableException_whenIdentityServiceRequestFails() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("did:example:alice");
        when(claims.get("token_type", String.class)).thenReturn("refresh");
        when(jwtService.parseToken("refresh-token")).thenReturn(claims);
        when(requestHeadersUriSpec.uri("/did/{did}", "did:example:alice"))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(responseSpec.bodyToMono(DidDocument.class)).thenReturn(Mono.error(
                new WebClientRequestException(
                        new IOException("connection reset"),
                        HttpMethod.GET,
                        URI.create("http://identity-service:8081/did/did:example:alice"),
                        HttpHeaders.EMPTY
                )));

        Mono<AuthResponse> refreshMono = service.refreshAccessToken("refresh-token");
        assertThatThrownBy(refreshMono::block)
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining("Identity service request failed");
    }
}
