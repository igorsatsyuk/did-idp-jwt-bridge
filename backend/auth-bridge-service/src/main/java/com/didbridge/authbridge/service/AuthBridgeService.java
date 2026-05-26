package com.didbridge.authbridge.service;

import com.didbridge.authbridge.dto.AuthRequest;
import com.didbridge.authbridge.dto.AuthResponse;
import com.didbridge.model.DidStatus;
import com.didbridge.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Authenticates a DID holder by:
 * 1. Resolving DID status from Identity Service
 * 2. Verifying the cryptographic signature against the DID's public key
 * 3. Issuing a JWT on success
 *
 * TODO: Replace HTTP call with direct Web3j call to DidRegistry contract.
 * TODO: Implement real ECDSA signature verification.
 */
@Service
public class AuthBridgeService {

    private final WebClient identityClient;
    private final JwtService jwtService;
    private final SignatureVerifier signatureVerifier;

    public AuthBridgeService(
            WebClient.Builder webClientBuilder,
            JwtService jwtService,
            SignatureVerifier signatureVerifier,
            @org.springframework.beans.factory.annotation.Value("${services.identity-url}") String identityUrl
    ) {
        this.identityClient = webClientBuilder.baseUrl(identityUrl).build();
        this.jwtService = jwtService;
        this.signatureVerifier = signatureVerifier;
    }

    public Mono<AuthResponse> authenticate(AuthRequest request) {
        return identityClient.get()
                .uri("/did/{did}", request.did())
                .retrieve()
                .bodyToMono(com.didbridge.model.DidDocument.class)
                .flatMap(doc -> {
                    if (doc.status() != DidStatus.ACTIVE) {
                        return Mono.error(new IllegalStateException("DID is not active"));
                    }
                    if (!signatureVerifier.verify(request.challenge(), request.signature(), doc.publicKey())) {
                        return Mono.error(new IllegalArgumentException("Invalid signature"));
                    }
                    String token = jwtService.generateToken(request.did(), Map.of("did", request.did()));
                    return Mono.just(new AuthResponse(token, "Bearer", 3600));
                });
    }

    public Mono<String> generateChallenge() {
        return Mono.just(UUID.randomUUID().toString());
    }
}
