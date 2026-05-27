package com.didbridge.identity.service;

import com.didbridge.model.DidDocument;
import com.didbridge.model.DidStatus;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of DidRegistryService.
 * Useful as a test double; not registered as a Spring bean.
 */
public class InMemoryDidRegistryService implements DidRegistryService {

    private final ConcurrentHashMap<String, DidDocument> store = new ConcurrentHashMap<>();

    @Override
    public Mono<DidDocument> register(String did, String publicKey) {
        Instant now = Instant.now();
        DidDocument doc = new DidDocument(did, publicKey, DidStatus.ACTIVE, now, now);
        DidDocument existing = store.putIfAbsent(did, doc);
        if (existing != null) {
            return Mono.error(new DidAlreadyRegisteredException(did, null));
        }
        return Mono.just(doc);
    }

    @Override
    public Mono<DidDocument> findByDid(String did) {
        return Mono.justOrEmpty(store.get(did))
                .switchIfEmpty(Mono.error(new DidNotFoundException(did)));
    }

    @Override
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
