package lt.satsyuk.didbridge.identity.controller;

import lt.satsyuk.didbridge.identity.dto.RegisterDidRequest;
import lt.satsyuk.didbridge.identity.dto.UpdateDidKeyRequest;
import lt.satsyuk.didbridge.identity.service.DidRegistryService;
import lt.satsyuk.didbridge.identity.service.KeyRotationAuthorizationException;
import lt.satsyuk.didbridge.model.DidDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/did")
public class DidController {

    private static final String KEY_ROTATION_HEADER = "X-Key-Rotation-Token";

    private final DidRegistryService didRegistryService;
    private final String keyRotationToken;

    public DidController(
            DidRegistryService didRegistryService,
            @Value("${security.key-rotation-token}") String keyRotationToken
    ) {
        this.didRegistryService = didRegistryService;
        this.keyRotationToken = keyRotationToken;
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
    public Mono<DidDocument> updateKey(
            @PathVariable String did,
            @RequestBody UpdateDidKeyRequest request,
            @RequestHeader(name = KEY_ROTATION_HEADER, required = false) String requestToken
    ) {
        ensureKeyRotationAuthorized(requestToken);
        return didRegistryService.updatePublicKey(did, request.publicKey());
    }

    private void ensureKeyRotationAuthorized(String requestToken) {
        if (requestToken == null || requestToken.isBlank()) {
            throw new KeyRotationAuthorizationException();
        }
        if (!MessageDigest.isEqual(
                keyRotationToken.getBytes(StandardCharsets.UTF_8),
                requestToken.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new KeyRotationAuthorizationException();
        }
    }
}

