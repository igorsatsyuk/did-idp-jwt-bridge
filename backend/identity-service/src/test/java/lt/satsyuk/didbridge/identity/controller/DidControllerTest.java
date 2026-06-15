package lt.satsyuk.didbridge.identity.controller;

import lt.satsyuk.didbridge.identity.dto.RegisterDidRequest;
import lt.satsyuk.didbridge.identity.dto.UpdateDidKeyRequest;
import lt.satsyuk.didbridge.identity.service.DidRegistryService;
import lt.satsyuk.didbridge.identity.service.KeyRotationAuthorizationException;
import lt.satsyuk.didbridge.model.DidDocument;
import lt.satsyuk.didbridge.model.DidStatus;
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
    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private DidRegistryService didRegistryService;

    @Test
    void register_delegatesToService() {
        DidController controller = new DidController(didRegistryService, KEY_ROTATION_TOKEN);
        RegisterDidRequest request = new RegisterDidRequest("did:example:alice", "0xpub");
        DidDocument expected = new DidDocument(
                request.did(), request.publicKey(), DidStatus.ACTIVE, FIXED_INSTANT, FIXED_INSTANT);
        when(didRegistryService.register(request.did(), request.publicKey())).thenReturn(Mono.just(expected));

        DidDocument result = controller.register(request).block();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getById_delegatesToService() {
        DidController controller = new DidController(didRegistryService, KEY_ROTATION_TOKEN);
        DidDocument expected = new DidDocument(
                "did:example:alice", "0xpub", DidStatus.ACTIVE, FIXED_INSTANT, FIXED_INSTANT);
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
                "did:example:alice", request.publicKey(), DidStatus.ACTIVE, FIXED_INSTANT, FIXED_INSTANT);
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

