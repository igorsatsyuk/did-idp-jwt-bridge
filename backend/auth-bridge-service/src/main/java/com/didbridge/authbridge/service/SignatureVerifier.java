package com.didbridge.authbridge.service;

import org.springframework.stereotype.Component;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;

/**
 * Verifies Ethereum-style ECDSA signatures.
 * The signer signs keccak256("\x19Ethereum Signed Message:\n" + message).
 */
@Component
public class SignatureVerifier {

    private static final byte[] EMPTY_BYTES = new byte[0];

    public boolean verify(String message, String signatureHex, String expectedSignerHex) {
        if (message == null || signatureHex == null || expectedSignerHex == null) {
            return false;
        }

        byte[] signatureBytes = parseHex(signatureHex);
        if (signatureBytes.length != 65) {
            return false;
        }

        Byte normalizedV = normalizeRecoveryId(signatureBytes[64]);
        if (normalizedV == null) {
            return false;
        }

        String expectedAddress = resolveExpectedAddress(expectedSignerHex);
        if (expectedAddress == null) {
            return false;
        }

        Sign.SignatureData signatureData = new Sign.SignatureData(
                normalizedV,
                Arrays.copyOfRange(signatureBytes, 0, 32),
                Arrays.copyOfRange(signatureBytes, 32, 64)
        );

        try {
            BigInteger recoveredKey = Sign.signedPrefixedMessageToKey(
                    message.getBytes(StandardCharsets.UTF_8), signatureData);
            String recoveredAddress = "0x" + Keys.getAddress(recoveredKey);
            return recoveredAddress.equalsIgnoreCase(expectedAddress);
        } catch (SignatureException _) {
            return false;
        }
    }

    private static byte[] parseHex(String value) {
        try {
            return Numeric.hexStringToByteArray(value);
        } catch (RuntimeException _) {
            return EMPTY_BYTES;
        }
    }

    private static Byte normalizeRecoveryId(byte v) {
        int recoveryId = Byte.toUnsignedInt(v);
        if (recoveryId == 27 || recoveryId == 28) {
            return (byte) recoveryId;
        }
        if (recoveryId == 0 || recoveryId == 1) {
            return (byte) (recoveryId + 27);
        }
        return null;
    }

    private static String resolveExpectedAddress(String expectedSignerHex) {
        String cleanHex = Numeric.cleanHexPrefix(expectedSignerHex);
        if (!isHex(cleanHex)) {
            return null;
        }

        if (cleanHex.length() == 40) {
            return "0x" + cleanHex;
        }

        String normalizedPublicKey = cleanHex;
        if (normalizedPublicKey.length() == 130 && normalizedPublicKey.startsWith("04")) {
            normalizedPublicKey = normalizedPublicKey.substring(2);
        }
        if (normalizedPublicKey.length() > 128) {
            return null;
        }
        normalizedPublicKey = leftPadToLength(normalizedPublicKey, 128);

        try {
            return "0x" + Keys.getAddress(new BigInteger(normalizedPublicKey, 16));
        } catch (RuntimeException _) {
            return null;
        }
    }

    private static boolean isHex(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) == -1) {
                return false;
            }
        }
        return true;
    }

    private static String leftPadToLength(String value, int expectedLength) {
        if (value.length() >= expectedLength) {
            return value;
        }
        return "0".repeat(expectedLength - value.length()) + value;
    }
}
