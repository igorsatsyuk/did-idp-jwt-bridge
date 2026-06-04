package lt.satsyuk.didbridge.authbridge.service;

import lt.satsyuk.didbridge.authbridge.dto.AuthRequest;
import lt.satsyuk.didbridge.authbridge.dto.AuthResponse;
import lt.satsyuk.didbridge.model.DidDocument;
import lt.satsyuk.didbridge.model.DidStatus;
import lt.satsyuk.didbridge.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Authenticates a DID holder by:
 * 1. Resolving DID status from Identity Service
 * 2. Verifying the cryptographic signature against the DID public key
 * 3. Issuing a JWT on success
 */
@Service
public class AuthBridgeService {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final WebClient identityClient;
    private final JwtService jwtService;
    private final SignatureVerifier signatureVerifier;
    private final ChallengeService challengeService;
    private final AuthTokenRateLimiter authTokenRateLimiter;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationMinutes;

    public AuthBridgeService(
            WebClient.Builder webClientBuilder,
            JwtService jwtService,
            SignatureVerifier signatureVerifier,
            ChallengeService challengeService,
            AuthTokenRateLimiter authTokenRateLimiter,
            @Value("${jwt.expiration-minutes:60}") long accessTokenExpirationMinutes,
            @Value("${jwt.refresh-expiration-minutes:10080}") long refreshTokenExpirationMinutes,
            @Value("${services.identity-url}") String identityUrl
    ) {
        this.identityClient = webClientBuilder.baseUrl(identityUrl).build();
        this.jwtService = jwtService;
        this.signatureVerifier = signatureVerifier;
        this.challengeService = challengeService;
        this.authTokenRateLimiter = authTokenRateLimiter;
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationMinutes = refreshTokenExpirationMinutes;
    }

    public Mono<AuthResponse> authenticate(AuthRequest request, String clientAddress) {
        return Mono.fromRunnable(() -> {
                    authTokenRateLimiter.enforceOrThrow(rateLimitKey(clientAddress));
                    challengeService.ensureActiveOrThrow(request.challenge());
                })
                .then(Mono.defer(() -> identityClient.get()
                        .uri("/did/{did}", request.did())
                        .retrieve()
                        .bodyToMono(DidDocument.class)))
                .flatMap(doc -> {
                    if (doc.status() == DidStatus.REVOKED) {
                        return Mono.error(new DidRevokedException(request.did()));
                    }
                    if (doc.status() != DidStatus.ACTIVE) {
                        return Mono.error(new IllegalStateException("Unsupported DID status: " + doc.status()));
                    }
                    if (!signatureVerifier.verify(request.challenge(), request.signature(), doc.publicKey())) {
                        return Mono.error(new IllegalArgumentException("Invalid signature"));
                    }
                    challengeService.consumeOrThrow(request.challenge());
                    return Mono.just(buildTokenResponse(request.did()));
                });
    }

    public Mono<String> generateChallenge() {
        return Mono.fromSupplier(challengeService::issueChallenge);
    }

    public Mono<AuthResponse> refreshAccessToken(String refreshToken) {
        return Mono.fromCallable(() -> jwtService.parseToken(refreshToken))
                .onErrorMap(ex -> new InvalidRefreshTokenException("Refresh token is invalid"))
                .flatMap(claims -> {
                    String did = claims.getSubject();
                    if (did == null || did.isBlank()) {
                        return Mono.error(new InvalidRefreshTokenException("Refresh token does not contain DID"));
                    }
                    String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
                    if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
                        return Mono.error(new InvalidRefreshTokenException("Token is not a refresh token"));
                    }
                    return identityClient.get()
                            .uri("/did/{did}", did)
                            .retrieve()
                            .bodyToMono(DidDocument.class)
                            .onErrorMap(WebClientRequestException.class,
                                    ex -> new IdentityServiceUnavailableException("Identity service request failed", ex))
                            .onErrorMap(WebClientResponseException.class, ex -> ex.getStatusCode().is4xxClientError()
                                    ? new InvalidRefreshTokenException("Refresh token is invalid")
                                    : new IdentityServiceUnavailableException("Identity service request failed", ex))
                            .flatMap(doc -> {
                                if (doc.status() == DidStatus.REVOKED) {
                                    return Mono.error(new DidRevokedException(did));
                                }
                                return Mono.just(buildTokenResponse(did));
                            });
                });
    }

    private static String rateLimitKey(String clientAddress) {
        return clientAddress;
    }

    private AuthResponse buildTokenResponse(String did) {
        String accessToken = jwtService.generateToken(
                did,
                Map.of("did", did),
                accessTokenExpirationMinutes
        );
        String refreshToken = jwtService.generateToken(
                did,
                Map.of("did", did, CLAIM_TOKEN_TYPE, REFRESH_TOKEN_TYPE),
                refreshTokenExpirationMinutes
        );
        return new AuthResponse(
                accessToken,
                "Bearer",
                accessTokenExpirationMinutes * 60,
                refreshToken,
                refreshTokenExpirationMinutes * 60
        );
    }
}

