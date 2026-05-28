package com.didbridge.authbridge.service;

import org.springframework.stereotype.Component;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Verifies Ethereum-style ECDSA signatures.
 * The signer signs keccak256("\x19Ethereum Signed Message:\n" + message).
 */
@Component
public class SignatureVerifier {

    public boolean verify(String message, String signatureHex, String expectedPublicKeyHex) {
        try {
            byte[] messageBytes = message.getBytes();
            byte[] signatureBytes = Numeric.hexStringToByteArray(signatureHex);

            byte v = signatureBytes[64];
            if (v < 27) v += 27;

            Sign.SignatureData signatureData = new Sign.SignatureData(
                    v,
                    Arrays.copyOfRange(signatureBytes, 0, 32),
                    Arrays.copyOfRange(signatureBytes, 32, 64)
            );

            BigInteger recoveredKey = Sign.signedPrefixedMessageToKey(messageBytes, signatureData);
            String recoveredAddress = "0x" + Keys.getAddress(recoveredKey);
            String expectedAddress = "0x" + Keys.getAddress(Numeric.toBigInt(expectedPublicKeyHex));

            return recoveredAddress.equalsIgnoreCase(expectedAddress);
        } catch (Exception _) {
            return false;
        }
    }
}
