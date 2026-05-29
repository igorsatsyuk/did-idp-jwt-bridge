package com.didbridge.identity.controller;

import com.didbridge.identity.dto.RegisterDidRequest;
import com.didbridge.identity.dto.UpdateDidKeyRequest;
import com.didbridge.identity.service.DidRegistryService;
import com.didbridge.model.DidDocument;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/did")
public class DidController {

    private final DidRegistryService didRegistryService;

    public DidController(DidRegistryService didRegistryService) {
        this.didRegistryService = didRegistryService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DidDocument> register(@RequestBody RegisterDidRequest request) {
        return didRegistryService.register(request.did(), request.publicKey());
    }

    @GetMapping("/{did}")
    public Mono<DidDocument> getById(@PathVariable String did) {
        return didRegistryService.findByDid(did);
    }

    @DeleteMapping("/{did}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> revoke(@PathVariable String did) {
        return didRegistryService.revoke(did);
    }

    @PutMapping("/{did}/key")
    public Mono<DidDocument> updateKey(@PathVariable String did, @RequestBody UpdateDidKeyRequest request) {
        return didRegistryService.updatePublicKey(did, request.publicKey());
    }
}
