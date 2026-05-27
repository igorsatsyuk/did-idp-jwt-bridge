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

    private static final String REVERT_DID_NOT_FOUND = "DID does not exist";
    private static final String REVERT_ALREADY_REGISTERED = "DID already registered";
    private static final String REVERT_NOT_OWNER = "Not the DID owner";

    private final DidRegistry contract;

    public Web3jDidRegistryService(DidRegistry contract) {
        this.contract = contract;
    }

    @Override
    public Mono<DidDocument> register(String did, String publicKey) {
        return Mono.fromCallable(() -> contract.registerDid(did, publicKey).send())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(ex -> containsRevert(ex, REVERT_ALREADY_REGISTERED),
                        ex -> new DidAlreadyRegisteredException(did, ex))
                .flatMap(receipt -> findByDid(did));
    }

    @Override
    public Mono<DidDocument> findByDid(String did) {
        RemoteFunctionCall<Tuple5<String, BigInteger, BigInteger, BigInteger, String>> call =
                contract.getDid(did);
        return Mono.fromCallable(call::send)
                .subscribeOn(Schedulers.boundedElastic())
                .map(tuple -> toDidDocument(did, tuple))
                .onErrorMap(ex -> containsRevert(ex, REVERT_DID_NOT_FOUND),
                        ex -> new DidNotFoundException(did, ex));
    }

    @Override
    public Mono<Void> revoke(String did) {
        return Mono.fromCallable(() -> contract.revokeDid(did).send())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(ex -> containsRevert(ex, REVERT_DID_NOT_FOUND),
                        ex -> new DidNotFoundException(did, ex))
                .onErrorMap(ex -> containsRevert(ex, REVERT_NOT_OWNER),
                        ex -> new DidOwnershipException(did, ex))
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

    /**
     * Walks the full cause chain to find a revert reason matching the given string.
     * Web3j may wrap the revert message in nested exceptions depending on the call type.
     */
    private static boolean containsRevert(Throwable ex, String revertReason) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains(revertReason)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
