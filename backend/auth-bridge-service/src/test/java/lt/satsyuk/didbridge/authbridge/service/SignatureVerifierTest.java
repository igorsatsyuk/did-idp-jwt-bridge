package lt.satsyuk.didbridge.authbridge.service;

import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureVerifierTest {

    private static final BigInteger PRIVATE_KEY = Numeric.toBigInt(
            "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80");

    private final SignatureVerifier verifier = new SignatureVerifier();

    @Test
    void verify_returnsTrue_forValidPrefixedSignature_withKnownKeyPair() {
        ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
        String message = "challenge-123";
        Sign.SignatureData sig = Sign.signPrefixedMessage(message.getBytes(StandardCharsets.UTF_8), keyPair);

        String signatureHex = toSignatureHex(sig);
        String expectedPublicKey = "0x" + keyPair.getPublicKey().toString(16);

        assertThat(verifier.verify(message, signatureHex, expectedPublicKey)).isTrue();
    }

    @Test
    void verify_returnsFalse_forInvalidSignatureLength() {
        ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
        byte[] invalidLengthSignature = new byte[64];
        String expectedPublicKey = "0x" + keyPair.getPublicKey().toString(16);

        assertThat(verifier.verify("challenge", Numeric.toHexString(invalidLengthSignature), expectedPublicKey)).isFalse();
    }

    @Test
    void verify_returnsFalse_forInvalidRecoveryIdByte() {
        ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
        Sign.SignatureData sig = Sign.signPrefixedMessage("challenge".getBytes(StandardCharsets.UTF_8), keyPair);
        byte[] signature = signatureToBytes(sig);
        signature[64] = 5;

        String expectedPublicKey = "0x" + keyPair.getPublicKey().toString(16);
        assertThat(verifier.verify("challenge", Numeric.toHexString(signature), expectedPublicKey)).isFalse();
    }

    @Test
    void verify_returnsFalse_whenMessageDiffersFromSignedMessage() {
        ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
        Sign.SignatureData sig = Sign.signPrefixedMessage("challenge-123".getBytes(StandardCharsets.UTF_8), keyPair);
        String signatureHex = toSignatureHex(sig);
        String expectedPublicKey = "0x" + keyPair.getPublicKey().toString(16);

        assertThat(verifier.verify("another-challenge", signatureHex, expectedPublicKey)).isFalse();
    }

    @Test
    void verify_returnsFalse_forNonHexExpectedPublicKey() {
        ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
        Sign.SignatureData sig = Sign.signPrefixedMessage("challenge-123".getBytes(StandardCharsets.UTF_8), keyPair);
        String signatureHex = toSignatureHex(sig);

        assertThat(verifier.verify("challenge-123", signatureHex, "0xnot-hex")).isFalse();
    }

    @Test
    void verify_acceptsExpectedEthereumAddress() {
        ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
        String message = "challenge-123";
        Sign.SignatureData sig = Sign.signPrefixedMessage(message.getBytes(StandardCharsets.UTF_8), keyPair);
        String signatureHex = toSignatureHex(sig);
        String expectedAddress = "0x" + Keys.getAddress(keyPair.getPublicKey());

        assertThat(verifier.verify(message, signatureHex, expectedAddress)).isTrue();
    }

    @Test
    void verify_acceptsUncompressedPublicKeyWith04Prefix() {
        ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
        String message = "challenge-123";
        Sign.SignatureData sig = Sign.signPrefixedMessage(message.getBytes(StandardCharsets.UTF_8), keyPair);
        String signatureHex = toSignatureHex(sig);
        String xAndY = Numeric.toHexStringNoPrefixZeroPadded(keyPair.getPublicKey(), 128);
        String expectedUncompressed = "0x04" + xAndY;

        assertThat(verifier.verify(message, signatureHex, expectedUncompressed)).isTrue();
    }

    @Test
    void verify_acceptsRecoveryIdInRangeZeroOne() {
        ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
        String message = "challenge-123";
        Sign.SignatureData sig = Sign.signPrefixedMessage(message.getBytes(StandardCharsets.UTF_8), keyPair);
        byte[] bytes = signatureToBytes(sig);
        bytes[64] = (byte) (bytes[64] - 27);
        String signatureHex = Numeric.toHexString(bytes);
        String expectedAddress = "0x" + Keys.getAddress(keyPair.getPublicKey());

        assertThat(verifier.verify(message, signatureHex, expectedAddress)).isTrue();
    }

    @Test
    void verify_returnsFalse_forNullInputs() {
        assertThat(verifier.verify(null, "0x1234", "0xabc")).isFalse();
        assertThat(verifier.verify("challenge", null, "0xabc")).isFalse();
        assertThat(verifier.verify("challenge", "0x1234", null)).isFalse();
    }

    @Test
    void verify_returnsFalse_forInvalidHexSignature() {
        ECKeyPair keyPair = ECKeyPair.create(PRIVATE_KEY);
        String expectedPublicKey = "0x" + keyPair.getPublicKey().toString(16);

        assertThat(verifier.verify("challenge", "0xzz", expectedPublicKey)).isFalse();
    }

    @Test
    void verify_acceptsShortPublicKeyHexByLeftPadding() {
        ECKeyPair keyPair = findKeyPairWithShortPublicKeyHex();
        String message = "challenge-short-key";
        Sign.SignatureData sig = Sign.signPrefixedMessage(message.getBytes(StandardCharsets.UTF_8), keyPair);
        String signatureHex = toSignatureHex(sig);
        String shortPublicKeyHex = "0x" + keyPair.getPublicKey().toString(16);

        assertThat(shortPublicKeyHex.length()).isLessThan("0x".length() + 128);
        assertThat(verifier.verify(message, signatureHex, shortPublicKeyHex)).isTrue();
    }

    private static String toSignatureHex(Sign.SignatureData sig) {
        return Numeric.toHexString(signatureToBytes(sig));
    }

    private static byte[] signatureToBytes(Sign.SignatureData sig) {
        byte[] bytes = new byte[65];
        System.arraycopy(sig.getR(), 0, bytes, 0, 32);
        System.arraycopy(sig.getS(), 0, bytes, 32, 32);
        bytes[64] = sig.getV()[0];
        return bytes;
    }

    private static ECKeyPair findKeyPairWithShortPublicKeyHex() {
        for (int i = 1; i <= 10_000; i++) {
            ECKeyPair keyPair = ECKeyPair.create(BigInteger.valueOf(i));
            if (keyPair.getPublicKey().toString(16).length() < 128) {
                return keyPair;
            }
        }
        throw new IllegalStateException("Could not find key pair with short public key hex");
    }
}

