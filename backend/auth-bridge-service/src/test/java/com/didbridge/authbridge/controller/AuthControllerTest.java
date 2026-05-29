package com.didbridge.authbridge.controller;

import com.didbridge.authbridge.dto.AuthRequest;
import com.didbridge.authbridge.dto.AuthResponse;
import com.didbridge.authbridge.dto.RefreshTokenRequest;
import com.didbridge.authbridge.service.AuthBridgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthBridgeService authBridgeService;
    @Mock
    private ServerHttpRequest serverHttpRequest;

    @Test
    void token_delegatesToService() {
        AuthController controller = new AuthController(authBridgeService);
        AuthRequest request = new AuthRequest("did:example:alice", "challenge", "0xsignature");
        AuthResponse response = new AuthResponse("jwt", "Bearer", 3600, "refresh", 604800);
        when(serverHttpRequest.getHeaders()).thenReturn(HttpHeaders.EMPTY);
        when(serverHttpRequest.getRemoteAddress()).thenReturn(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 12345));
        when(authBridgeService.authenticate(request, "127.0.0.1")).thenReturn(Mono.just(response));

        AuthResponse result = controller.token(request, serverHttpRequest).block();

        assertThat(result).isEqualTo(response);
    }

    @Test
    void token_usesUnknownClient_whenRemoteAddressMissing() {
        AuthController controller = new AuthController(authBridgeService);
        AuthRequest request = new AuthRequest("did:example:alice", "challenge", "0xsignature");
        AuthResponse response = new AuthResponse("jwt", "Bearer", 3600, "refresh", 604800);
        when(serverHttpRequest.getHeaders()).thenReturn(HttpHeaders.EMPTY);
        when(serverHttpRequest.getRemoteAddress()).thenReturn(null);
        when(authBridgeService.authenticate(request, "unknown-client")).thenReturn(Mono.just(response));

        AuthResponse result = controller.token(request, serverHttpRequest).block();

        assertThat(result).isEqualTo(response);
    }

    @Test
    void token_usesUnknownClient_whenRemoteAddressIsUnresolved() {
        AuthController controller = new AuthController(authBridgeService);
        AuthRequest request = new AuthRequest("did:example:alice", "challenge", "0xsignature");
        AuthResponse response = new AuthResponse("jwt", "Bearer", 3600, "refresh", 604800);
        when(serverHttpRequest.getHeaders()).thenReturn(HttpHeaders.EMPTY);
        when(serverHttpRequest.getRemoteAddress()).thenReturn(InetSocketAddress.createUnresolved("client", 12345));
        when(authBridgeService.authenticate(request, "unknown-client")).thenReturn(Mono.just(response));

        AuthResponse result = controller.token(request, serverHttpRequest).block();

        assertThat(result).isEqualTo(response);
    }

    @Test
    void token_usesXForwardedFor_whenPresent() {
        AuthController controller = new AuthController(authBridgeService);
        AuthRequest request = new AuthRequest("did:example:alice", "challenge", "0xsignature");
        AuthResponse response = new AuthResponse("jwt", "Bearer", 3600, "refresh", 604800);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", "203.0.113.7, 10.0.0.2");
        when(serverHttpRequest.getHeaders()).thenReturn(headers);
        when(authBridgeService.authenticate(request, "203.0.113.7")).thenReturn(Mono.just(response));

        AuthResponse result = controller.token(request, serverHttpRequest).block();

        assertThat(result).isEqualTo(response);
    }

    @Test
    void token_usesForwardedHeader_whenXForwardedForMissing() {
        AuthController controller = new AuthController(authBridgeService);
        AuthRequest request = new AuthRequest("did:example:alice", "challenge", "0xsignature");
        AuthResponse response = new AuthResponse("jwt", "Bearer", 3600, "refresh", 604800);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Forwarded", "for=203.0.113.9;proto=https;by=203.0.113.43");
        when(serverHttpRequest.getHeaders()).thenReturn(headers);
        when(authBridgeService.authenticate(request, "203.0.113.9")).thenReturn(Mono.just(response));

        AuthResponse result = controller.token(request, serverHttpRequest).block();

        assertThat(result).isEqualTo(response);
    }

    @Test
    void challenge_delegatesToService() {
        AuthController controller = new AuthController(authBridgeService);
        when(authBridgeService.generateChallenge()).thenReturn(Mono.just("abc"));

        String result = controller.challenge().block();

        assertThat(result).isEqualTo("abc");
    }

    @Test
    void refresh_delegatesToService() {
        AuthController controller = new AuthController(authBridgeService);
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        AuthResponse response = new AuthResponse("jwt", "Bearer", 3600, "refresh", 604800);
        when(authBridgeService.refreshAccessToken(request.refreshToken())).thenReturn(Mono.just(response));

        AuthResponse result = controller.refresh(request).block();

        assertThat(result).isEqualTo(response);
    }
}
