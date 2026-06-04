package lt.satsyuk.didbridge.authbridge.controller;

import lt.satsyuk.didbridge.authbridge.dto.AuthRequest;
import lt.satsyuk.didbridge.authbridge.dto.AuthResponse;
import lt.satsyuk.didbridge.authbridge.dto.RefreshTokenRequest;
import lt.satsyuk.didbridge.authbridge.service.AuthBridgeService;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String UNKNOWN_CLIENT = "unknown-client";

    private final AuthBridgeService authBridgeService;

    public AuthController(AuthBridgeService authBridgeService) {
        this.authBridgeService = authBridgeService;
    }

    @PostMapping("/token")
    public Mono<AuthResponse> token(
            @RequestBody AuthRequest authRequest,
            ServerHttpRequest serverHttpRequest
    ) {
        return authBridgeService.authenticate(authRequest, resolveClientAddress(serverHttpRequest));
    }

    @PostMapping("/refresh")
    public Mono<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return authBridgeService.refreshAccessToken(request.refreshToken());
    }

    @GetMapping("/challenge")
    public Mono<String> challenge() {
        return authBridgeService.generateChallenge();
    }

    private static String resolveClientAddress(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return UNKNOWN_CLIENT;
        }
        return remoteAddress.getAddress().getHostAddress();
    }
}

