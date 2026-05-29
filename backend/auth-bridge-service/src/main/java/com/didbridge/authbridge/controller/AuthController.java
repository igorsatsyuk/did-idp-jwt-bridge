package com.didbridge.authbridge.controller;

import com.didbridge.authbridge.dto.AuthRequest;
import com.didbridge.authbridge.dto.AuthResponse;
import com.didbridge.authbridge.dto.RefreshTokenRequest;
import com.didbridge.authbridge.service.AuthBridgeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String UNKNOWN_CLIENT = "unknown-client";
    private static final String FORWARDED_HEADER = "Forwarded";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String FORWARDED_FOR_PREFIX = "for=";

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
        String forwardedForAddress = resolveForwardedAddress(request.getHeaders());
        if (forwardedForAddress != null) {
            return forwardedForAddress;
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return UNKNOWN_CLIENT;
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    private static String resolveForwardedAddress(HttpHeaders headers) {
        String xForwardedFor = headers.getFirst(X_FORWARDED_FOR_HEADER);
        String xForwardedForAddress = extractFirstAddressFromList(xForwardedFor);
        if (xForwardedForAddress != null) {
            return xForwardedForAddress;
        }

        String forwarded = headers.getFirst(FORWARDED_HEADER);
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }
        for (String forwardedPart : forwarded.split(";")) {
            String part = forwardedPart.trim();
            if (!part.regionMatches(true, 0, FORWARDED_FOR_PREFIX, 0, FORWARDED_FOR_PREFIX.length())) {
                continue;
            }
            String address = part.substring(FORWARDED_FOR_PREFIX.length()).trim();
            if (address.startsWith("\"") && address.endsWith("\"") && address.length() > 1) {
                address = address.substring(1, address.length() - 1);
            }
            if (address.startsWith("[") && address.endsWith("]") && address.length() > 1) {
                address = address.substring(1, address.length() - 1);
            }
            if (!address.isBlank()) {
                return address;
            }
        }
        return null;
    }

    private static String extractFirstAddressFromList(String addressesHeaderValue) {
        if (addressesHeaderValue == null || addressesHeaderValue.isBlank()) {
            return null;
        }
        String firstAddress = addressesHeaderValue.split(",")[0].trim();
        return firstAddress.isBlank() ? null : firstAddress;
    }
}
