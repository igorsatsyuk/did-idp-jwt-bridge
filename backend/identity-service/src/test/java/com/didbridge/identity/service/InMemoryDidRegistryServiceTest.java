package com.didbridge.identity.service;

import com.didbridge.model.DidDocument;
import com.didbridge.model.DidStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDidRegistryServiceTest {

    private InMemoryDidRegistryService service;

    private static final String DID = "did:example:abc";
    private static final String PUBLIC_KEY = "0x04xyz";

    @BeforeEach
    void setUp() {
        service = new InMemoryDidRegistryService();
    }

    @Test
    void register_storesAndReturnsActiveDocument() {
        DidDocument doc = service.register(DID, PUBLIC_KEY).block();

        assertThat(doc).isNotNull();
        assertThat(doc.did()).isEqualTo(DID);
        assertThat(doc.publicKey()).isEqualTo(PUBLIC_KEY);
        assertThat(doc.status()).isEqualTo(DidStatus.ACTIVE);
        assertThat(doc.createdAt()).isNotNull();
    }

    @Test
    void findByDid_returnsStoredDocument() {
        service.register(DID, PUBLIC_KEY).block();

        DidDocument doc = service.findByDid(DID).block();

        assertThat(doc).isNotNull();
        assertThat(doc.did()).isEqualTo(DID);
        assertThat(doc.status()).isEqualTo(DidStatus.ACTIVE);
    }

    @Test
    void findByDid_throwsDidNotFoundException_whenNotRegistered() {
        StepVerifier.create(service.findByDid(DID))
                .expectErrorMatches(ex -> ex instanceof DidNotFoundException
                        && ex.getMessage().contains(DID))
                .verify();
    }

    @Test
    void revoke_changesStatusToRevoked() {
        service.register(DID, PUBLIC_KEY).block();
        service.revoke(DID).block();

        DidDocument doc = service.findByDid(DID).block();

        assertThat(doc).isNotNull();
        assertThat(doc.status()).isEqualTo(DidStatus.REVOKED);
    }

    @Test
    void revoke_throwsDidNotFoundException_whenNotRegistered() {
        StepVerifier.create(service.revoke(DID))
                .expectErrorMatches(ex -> ex instanceof DidNotFoundException)
                .verify();
    }
}
