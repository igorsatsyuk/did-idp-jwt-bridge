package com.didbridge.authbridge.service;

import com.didbridge.authbridge.dto.AuthRequest;
import com.didbridge.authbridge.dto.AuthResponse;
import com.didbridge.model.DidDocument;
import com.didbridge.model.DidStatus;
import com.didbridge.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthBridgeServiceTest {

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
        when(jwtService.generateToken(request.did(), Map.of("did", request.did()))).thenReturn("jwt-token");

        AuthResponse response = service.authenticate(request).block();

        assertThat(response).isEqualTo(new AuthResponse("jwt-token", "Bearer", 3600));
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

        Mono<AuthResponse> authMono = service.authenticate(request);
        assertThatThrownBy(authMono::block)
                .isInstanceOf(DidRevokedException.class)
                .hasMessageContaining("revoked");
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

        Mono<AuthResponse> authMono = service.authenticate(request);
        assertThatThrownBy(authMono::block)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid signature");
        verify(challengeService).ensureActiveOrThrow(request.challenge());
        verify(challengeService, never()).consumeOrThrow(request.challenge());
    }

    @Test
    void authenticate_returnsUnauthorized_whenChallengeInvalidOrReplayed() {
        AuthRequest request = new AuthRequest("did:example:alice", "replayed", "0xsignature");
        doThrow(new InvalidChallengeException("Challenge is invalid, expired, or already used"))
                .when(challengeService).ensureActiveOrThrow(request.challenge());

        Mono<AuthResponse> authMono = service.authenticate(request);
        assertThatThrownBy(authMono::block)
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("invalid, expired, or already used");
    }

    @Test
    void generateChallenge_returnsUuidString() {
        String issued = UUID.randomUUID().toString();
        when(challengeService.issueChallenge()).thenReturn(issued);
        String challenge = service.generateChallenge().block();

        assertThat(challenge).isEqualTo(issued);
    }
}
