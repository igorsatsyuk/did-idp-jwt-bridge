package com.didbridge.identity.service;

import com.didbridge.identity.contract.DidRegistry;
import com.didbridge.model.DidDocument;
import com.didbridge.model.DidStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
import reactor.test.StepVerifier;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Web3jDidRegistryServiceTest {

    @Mock
    private DidRegistry contract;

    private Web3jDidRegistryService service;

    private static final String DID = "did:example:123";
    private static final String PUBLIC_KEY = "0x04abc";
    private static final BigInteger ACTIVE = BigInteger.ZERO;
    private static final BigInteger REVOKED = BigInteger.ONE;
    private static final BigInteger TIMESTAMP = BigInteger.valueOf(1_700_000_000L);

    @BeforeEach
    void setUp() {
        service = new Web3jDidRegistryService(contract);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByDid_returnsActiveDidDocument() throws Exception {
        Tuple5<String, BigInteger, BigInteger, BigInteger, String> tuple =
                new Tuple5<>(PUBLIC_KEY, ACTIVE, TIMESTAMP, TIMESTAMP, "0xowner");

        RemoteFunctionCall<Tuple5<String, BigInteger, BigInteger, BigInteger, String>> call =
                mock(RemoteFunctionCall.class);
        when(contract.getDid(DID)).thenReturn(call);
        when(call.send()).thenReturn(tuple);

        DidDocument doc = service.findByDid(DID).block();

        assertThat(doc).isNotNull();
        assertThat(doc.did()).isEqualTo(DID);
        assertThat(doc.publicKey()).isEqualTo(PUBLIC_KEY);
        assertThat(doc.status()).isEqualTo(DidStatus.ACTIVE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByDid_returnsRevokedDidDocument() throws Exception {
        Tuple5<String, BigInteger, BigInteger, BigInteger, String> tuple =
                new Tuple5<>(PUBLIC_KEY, REVOKED, TIMESTAMP, TIMESTAMP, "0xowner");

        RemoteFunctionCall<Tuple5<String, BigInteger, BigInteger, BigInteger, String>> call =
                mock(RemoteFunctionCall.class);
        when(contract.getDid(DID)).thenReturn(call);
        when(call.send()).thenReturn(tuple);

        DidDocument doc = service.findByDid(DID).block();

        assertThat(doc).isNotNull();
        assertThat(doc.status()).isEqualTo(DidStatus.REVOKED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByDid_throwsDidNotFoundException_whenContractReverts() throws Exception {
        RemoteFunctionCall<Tuple5<String, BigInteger, BigInteger, BigInteger, String>> call =
                mock(RemoteFunctionCall.class);
        when(contract.getDid(DID)).thenReturn(call);
        when(call.send()).thenThrow(new RuntimeException("DID does not exist"));

        StepVerifier.create(service.findByDid(DID))
                .expectErrorMatches(ex -> ex instanceof DidNotFoundException
                        && ex.getMessage().contains(DID))
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void register_callsContractAndReturnsDid() throws Exception {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        RemoteFunctionCall<TransactionReceipt> txCall = mock(RemoteFunctionCall.class);
        when(contract.registerDid(DID, PUBLIC_KEY)).thenReturn(txCall);
        when(txCall.send()).thenReturn(receipt);

        Tuple5<String, BigInteger, BigInteger, BigInteger, String> tuple =
                new Tuple5<>(PUBLIC_KEY, ACTIVE, TIMESTAMP, TIMESTAMP, "0xowner");
        RemoteFunctionCall<Tuple5<String, BigInteger, BigInteger, BigInteger, String>> getCall =
                mock(RemoteFunctionCall.class);
        when(contract.getDid(DID)).thenReturn(getCall);
        when(getCall.send()).thenReturn(tuple);

        DidDocument doc = service.register(DID, PUBLIC_KEY).block();

        assertThat(doc).isNotNull();
        assertThat(doc.did()).isEqualTo(DID);
        assertThat(doc.status()).isEqualTo(DidStatus.ACTIVE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void revoke_completesSuccessfully() throws Exception {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        RemoteFunctionCall<TransactionReceipt> call = mock(RemoteFunctionCall.class);
        when(contract.revokeDid(DID)).thenReturn(call);
        when(call.send()).thenReturn(receipt);

        StepVerifier.create(service.revoke(DID))
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void revoke_throwsDidNotFoundException_whenContractReverts() throws Exception {
        RemoteFunctionCall<TransactionReceipt> call = mock(RemoteFunctionCall.class);
        when(contract.revokeDid(DID)).thenReturn(call);
        when(call.send()).thenThrow(new RuntimeException("DID does not exist"));

        StepVerifier.create(service.revoke(DID))
                .expectErrorMatches(DidNotFoundException.class::isInstance)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByDid_propagatesOtherExceptions() throws Exception {
        RemoteFunctionCall<Tuple5<String, BigInteger, BigInteger, BigInteger, String>> call =
                mock(RemoteFunctionCall.class);
        when(contract.getDid(DID)).thenReturn(call);
        when(call.send()).thenThrow(new RuntimeException("network error"));

        StepVerifier.create(service.findByDid(DID))
                .expectErrorMatches(ex -> ex instanceof RuntimeException
                        && ex.getMessage().equals("network error"))
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void revoke_propagatesOtherExceptions() throws Exception {
        RemoteFunctionCall<TransactionReceipt> call = mock(RemoteFunctionCall.class);
        when(contract.revokeDid(DID)).thenReturn(call);
        when(call.send()).thenThrow(new RuntimeException("network error"));

        StepVerifier.create(service.revoke(DID))
                .expectErrorMatches(ex -> ex instanceof RuntimeException
                        && ex.getMessage().equals("network error"))
                .verify();
    }
}
