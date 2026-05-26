package com.didbridge.identity.service;

import com.didbridge.model.DidDocument;
import com.didbridge.model.DidStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing DID registrations via DidRegistry smart contract.
 * TODO: Replace in-memory map with real Web3j calls to DidRegistry contract (issue #3).
 */
@Service
public class DidRegistryService {

    private final ConcurrentHashMap<String, DidDocument> store = new ConcurrentHashMap<>();

    public Mono<DidDocument> register(String did, String publicKey) {
        DidDocument doc = new DidDocument(did, publicKey, DidStatus.ACTIVE, Instant.now(), Instant.now());
        store.put(did, doc);
        return Mono.just(doc);
    }

    public Mono<DidDocument> findByDid(String did) {
        return Mono.justOrEmpty(store.get(did))
                .switchIfEmpty(Mono.error(new DidNotFoundException(did)));
    }

    public Mono<Void> revoke(String did) {
        return findByDid(did)
                .flatMap(doc -> {
                    DidDocument revoked = new DidDocument(
                            doc.did(), doc.publicKey(), DidStatus.REVOKED, doc.createdAt(), Instant.now()
                    );
                    store.put(did, revoked);
                    return Mono.<Void>empty();
                });
    }
}
