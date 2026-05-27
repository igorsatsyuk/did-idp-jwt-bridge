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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    void findByDid_createsFreshContractCallPerSubscription() throws Exception {
        Tuple5<String, BigInteger, BigInteger, BigInteger, String> tuple =
                new Tuple5<>(PUBLIC_KEY, ACTIVE, TIMESTAMP, TIMESTAMP, "0xowner");
        RemoteFunctionCall<Tuple5<String, BigInteger, BigInteger, BigInteger, String>> call1 =
                mock(RemoteFunctionCall.class);
        RemoteFunctionCall<Tuple5<String, BigInteger, BigInteger, BigInteger, String>> call2 =
                mock(RemoteFunctionCall.class);
        when(contract.getDid(DID)).thenReturn(call1, call2);
        when(call1.send()).thenReturn(tuple);
        when(call2.send()).thenReturn(tuple);

        Mono<DidDocument> mono = service.findByDid(DID);
        mono.block();
        mono.block();

        verify(contract, times(2)).getDid(DID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByDid_throwsDidNotFoundException_whenContractReverts() throws Exception {
        RemoteFunctionCall<Tuple5<String, BigInteger, BigInteger, BigInteger, String>> call =
                mock(RemoteFunctionCall.class);
        when(contract.getDid(DID)).thenReturn(call);
        RuntimeException cause = new RuntimeException("DID does not exist");
        when(call.send()).thenThrow(cause);

        StepVerifier.create(service.findByDid(DID))
                .expectErrorMatches(ex -> ex instanceof DidNotFoundException
                        && ex.getMessage().contains(DID)
                        && ex.getCause() == cause)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void register_callsContractAndReturnsDid() throws Exception {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getStatus()).thenReturn("0x1");
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
        when(receipt.getStatus()).thenReturn("0x1");
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
        RuntimeException cause = new RuntimeException("DID does not exist");
        when(call.send()).thenThrow(cause);

        StepVerifier.create(service.revoke(DID))
                .expectErrorMatches(ex -> ex instanceof DidNotFoundException && ex.getCause() == cause)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void register_throwsDidAlreadyRegisteredException_whenContractReverts() throws Exception {
        RemoteFunctionCall<TransactionReceipt> txCall = mock(RemoteFunctionCall.class);
        when(contract.registerDid(DID, PUBLIC_KEY)).thenReturn(txCall);
        RuntimeException cause = new RuntimeException("DID already registered");
        when(txCall.send()).thenThrow(cause);

        StepVerifier.create(service.register(DID, PUBLIC_KEY))
                .expectErrorMatches(ex -> ex instanceof DidAlreadyRegisteredException
                        && ex.getMessage().contains(DID)
                        && ex.getCause() == cause)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void register_throwsDidAlreadyRegisteredException_whenReceiptHasFailedStatus() throws Exception {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getStatus()).thenReturn("0x0");
        when(receipt.getRevertReason()).thenReturn("DID already registered");

        RemoteFunctionCall<TransactionReceipt> txCall = mock(RemoteFunctionCall.class);
        when(contract.registerDid(DID, PUBLIC_KEY)).thenReturn(txCall);
        when(txCall.send()).thenReturn(receipt);

        StepVerifier.create(service.register(DID, PUBLIC_KEY))
                .expectErrorMatches(DidAlreadyRegisteredException.class::isInstance)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void register_throwsDidAlreadyRegisteredException_whenReceiptReasonContainsPrefix() throws Exception {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getStatus()).thenReturn("0x0");
        when(receipt.getRevertReason()).thenReturn("execution reverted: DID already registered");

        RemoteFunctionCall<TransactionReceipt> txCall = mock(RemoteFunctionCall.class);
        when(contract.registerDid(DID, PUBLIC_KEY)).thenReturn(txCall);
        when(txCall.send()).thenReturn(receipt);

        StepVerifier.create(service.register(DID, PUBLIC_KEY))
                .expectErrorMatches(DidAlreadyRegisteredException.class::isInstance)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void register_throwsIllegalStateException_whenReceiptStatusIsNull() throws Exception {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getStatus()).thenReturn(null);

        RemoteFunctionCall<TransactionReceipt> txCall = mock(RemoteFunctionCall.class);
        when(contract.registerDid(DID, PUBLIC_KEY)).thenReturn(txCall);
        when(txCall.send()).thenReturn(receipt);

        StepVerifier.create(service.register(DID, PUBLIC_KEY))
                .expectErrorMatches(ex -> ex instanceof IllegalStateException
                        && ex.getMessage().contains("unknown status"))
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void revoke_throwsDidOwnershipException_whenNotOwner() throws Exception {
        RemoteFunctionCall<TransactionReceipt> call = mock(RemoteFunctionCall.class);
        when(contract.revokeDid(DID)).thenReturn(call);
        RuntimeException cause = new RuntimeException("Not the DID owner");
        when(call.send()).thenThrow(cause);

        StepVerifier.create(service.revoke(DID))
                .expectErrorMatches(ex -> ex instanceof DidOwnershipException
                        && ex.getMessage().contains(DID)
                        && ex.getCause() == cause)
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

    @Test
    @SuppressWarnings("unchecked")
    void revoke_throwsDidNotFoundException_whenReceiptHasFailedStatus() throws Exception {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getStatus()).thenReturn("0x0");
        when(receipt.getRevertReason()).thenReturn("DID does not exist");

        RemoteFunctionCall<TransactionReceipt> call = mock(RemoteFunctionCall.class);
        when(contract.revokeDid(DID)).thenReturn(call);
        when(call.send()).thenReturn(receipt);

        StepVerifier.create(service.revoke(DID))
                .expectErrorMatches(DidNotFoundException.class::isInstance)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void revoke_throwsDidOwnershipException_whenReceiptHasFailedStatus() throws Exception {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getStatus()).thenReturn("0x0");
        when(receipt.getRevertReason()).thenReturn("Not the DID owner");

        RemoteFunctionCall<TransactionReceipt> call = mock(RemoteFunctionCall.class);
        when(contract.revokeDid(DID)).thenReturn(call);
        when(call.send()).thenReturn(receipt);

        StepVerifier.create(service.revoke(DID))
                .expectErrorMatches(DidOwnershipException.class::isInstance)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void revoke_throwsDidOwnershipException_whenReceiptReasonContainsPrefix() throws Exception {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getStatus()).thenReturn("0x0");
        when(receipt.getRevertReason()).thenReturn("VM Exception while processing transaction: revert Not the DID owner");

        RemoteFunctionCall<TransactionReceipt> call = mock(RemoteFunctionCall.class);
        when(contract.revokeDid(DID)).thenReturn(call);
        when(call.send()).thenReturn(receipt);

        StepVerifier.create(service.revoke(DID))
                .expectErrorMatches(DidOwnershipException.class::isInstance)
                .verify();
    }
}
