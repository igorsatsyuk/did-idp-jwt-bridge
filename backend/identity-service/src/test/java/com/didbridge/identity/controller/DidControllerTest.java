package com.didbridge.identity.controller;

import com.didbridge.identity.dto.RegisterDidRequest;
import com.didbridge.identity.dto.UpdateDidKeyRequest;
import com.didbridge.identity.service.DidRegistryService;
import com.didbridge.identity.service.KeyRotationAuthorizationException;
import com.didbridge.model.DidDocument;
import com.didbridge.model.DidStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DidControllerTest {

    private static final String KEY_ROTATION_TOKEN = "test-key-rotation-token";

    @Mock
    private DidRegistryService didRegistryService;

    @Test
    void register_delegatesToService() {
        DidController controller = new DidController(didRegistryService, KEY_ROTATION_TOKEN);
        RegisterDidRequest request = new RegisterDidRequest("did:example:alice", "0xpub");
        DidDocument expected = new DidDocument(
                request.did(), request.publicKey(), DidStatus.ACTIVE, Instant.now(), Instant.now());
        when(didRegistryService.register(request.did(), request.publicKey())).thenReturn(Mono.just(expected));

        DidDocument result = controller.register(request).block();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getById_delegatesToService() {
        DidController controller = new DidController(didRegistryService, KEY_ROTATION_TOKEN);
        DidDocument expected = new DidDocument(
                "did:example:alice", "0xpub", DidStatus.ACTIVE, Instant.now(), Instant.now());
        when(didRegistryService.findByDid("did:example:alice")).thenReturn(Mono.just(expected));

        DidDocument result = controller.getById("did:example:alice").block();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void revoke_delegatesToService() {
        DidController controller = new DidController(didRegistryService, KEY_ROTATION_TOKEN);
        when(didRegistryService.revoke("did:example:alice")).thenReturn(Mono.empty());

        Void result = controller.revoke("did:example:alice").block();

        assertThat(result).isNull();
    }

    @Test
    void updateKey_delegatesToService() {
        DidController controller = new DidController(didRegistryService, KEY_ROTATION_TOKEN);
        UpdateDidKeyRequest request = new UpdateDidKeyRequest("0xnew-pub");
        DidDocument expected = new DidDocument(
                "did:example:alice", request.publicKey(), DidStatus.ACTIVE, Instant.now(), Instant.now());
        when(didRegistryService.updatePublicKey("did:example:alice", request.publicKey()))
                .thenReturn(Mono.just(expected));

        DidDocument result = controller.updateKey("did:example:alice", request, KEY_ROTATION_TOKEN).block();

        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"wrong-token", "   "})
    void updateKey_throwsWhenAuthorizationTokenInvalid(String requestToken) {
        DidController controller = new DidController(didRegistryService, KEY_ROTATION_TOKEN);
        UpdateDidKeyRequest request = new UpdateDidKeyRequest("0xnew-pub");

        assertThatThrownBy(() -> controller.updateKey("did:example:alice", request, requestToken))
                .isInstanceOf(KeyRotationAuthorizationException.class);
    }
}
