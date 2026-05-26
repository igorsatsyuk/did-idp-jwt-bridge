package com.didbridge.authbridge.controller;

import com.didbridge.authbridge.dto.AuthRequest;
import com.didbridge.authbridge.dto.AuthResponse;
import com.didbridge.authbridge.service.AuthBridgeService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthBridgeService authBridgeService;

    public AuthController(AuthBridgeService authBridgeService) {
        this.authBridgeService = authBridgeService;
    }

    @PostMapping("/token")
    public Mono<AuthResponse> token(@RequestBody AuthRequest request) {
        return authBridgeService.authenticate(request);
    }

    @GetMapping("/challenge")
    public Mono<String> challenge() {
        return authBridgeService.generateChallenge();
    }
}
