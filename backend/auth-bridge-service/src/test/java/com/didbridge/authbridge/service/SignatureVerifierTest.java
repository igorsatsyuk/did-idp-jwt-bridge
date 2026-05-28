package com.didbridge.authbridge.service;

import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureVerifierTest {

    private final SignatureVerifier verifier = new SignatureVerifier();

    @Test
    void verify_returnsTrue_forValidPrefixedSignature() {
        BigInteger privateKey = Numeric.toBigInt(
                "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80");
        ECKeyPair keyPair = ECKeyPair.create(privateKey);
        String message = "challenge-123";
        Sign.SignatureData sig = Sign.signPrefixedMessage(message.getBytes(), keyPair);

        String signatureHex = toSignatureHex(sig);
        String expectedPublicKey = "0x" + keyPair.getPublicKey().toString(16);

        assertThat(verifier.verify(message, signatureHex, expectedPublicKey)).isTrue();
    }

    @Test
    void verify_returnsFalse_forMalformedSignature() {
        String expectedPublicKey = "0x04abc";

        assertThat(verifier.verify("challenge", "0x1234", expectedPublicKey)).isFalse();
    }

    private static String toSignatureHex(Sign.SignatureData sig) {
        byte[] bytes = new byte[65];
        System.arraycopy(sig.getR(), 0, bytes, 0, 32);
        System.arraycopy(sig.getS(), 0, bytes, 32, 32);
        bytes[64] = sig.getV()[0];
        return Numeric.toHexString(bytes);
    }
}
