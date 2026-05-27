package com.didbridge.identity.service;

import com.didbridge.identity.contract.DidRegistry;
import com.didbridge.model.DidDocument;
import com.didbridge.model.DidStatus;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.tuples.generated.Tuple5;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigInteger;
import java.time.Instant;

@Service
public class Web3jDidRegistryService implements DidRegistryService {

    private final DidRegistry contract;

    public Web3jDidRegistryService(DidRegistry contract) {
        this.contract = contract;
    }

    @Override
    public Mono<DidDocument> register(String did, String publicKey) {
        return Mono.fromCallable(() -> contract.registerDid(did, publicKey).send())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(receipt -> findByDid(did));
    }

    @Override
    public Mono<DidDocument> findByDid(String did) {
        RemoteFunctionCall<Tuple5<String, BigInteger, BigInteger, BigInteger, String>> call =
                contract.getDid(did);
        return Mono.fromCallable(call::send)
                .subscribeOn(Schedulers.boundedElastic())
                .map(tuple -> toDidDocument(did, tuple))
                .onErrorMap(this::isDidNotFound, ex -> new DidNotFoundException(did));
    }

    @Override
    public Mono<Void> revoke(String did) {
        return Mono.fromCallable(() -> contract.revokeDid(did).send())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(this::isDidNotFound, ex -> new DidNotFoundException(did))
                .then();
    }

    private DidDocument toDidDocument(String did,
            Tuple5<String, BigInteger, BigInteger, BigInteger, String> t) {
        DidStatus status = BigInteger.ZERO.equals(t.component2()) ? DidStatus.ACTIVE : DidStatus.REVOKED;
        return new DidDocument(
                did,
                t.component1(),
                status,
                Instant.ofEpochSecond(t.component3().longValue()),
                Instant.ofEpochSecond(t.component4().longValue())
        );
    }

    private boolean isDidNotFound(Throwable ex) {
        return ex.getMessage() != null && ex.getMessage().contains("DID does not exist");
    }
}
