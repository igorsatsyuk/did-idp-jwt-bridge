package lt.satsyuk.didbridge.identity.service;

import lt.satsyuk.didbridge.model.DidDocument;
import reactor.core.publisher.Mono;

public interface DidRegistryService {
    Mono<DidDocument> register(String did, String publicKey);
    Mono<DidDocument> findByDid(String did);
    Mono<Void> revoke(String did);
    Mono<DidDocument> updatePublicKey(String did, String newPublicKey);
}

