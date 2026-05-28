package com.didbridge.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DidModelTest {

    @Test
    void didDocument_exposesAllRecordFields() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-01T01:00:00Z");
        DidDocument doc = new DidDocument(
                "did:example:alice",
                "0xpublic",
                DidStatus.ACTIVE,
                createdAt,
                updatedAt
        );

        assertThat(doc.did()).isEqualTo("did:example:alice");
        assertThat(doc.publicKey()).isEqualTo("0xpublic");
        assertThat(doc.status()).isEqualTo(DidStatus.ACTIVE);
        assertThat(doc.createdAt()).isEqualTo(createdAt);
        assertThat(doc.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void didStatus_containsExpectedValues() {
        assertThat(DidStatus.values()).containsExactly(DidStatus.ACTIVE, DidStatus.REVOKED);
    }
}
