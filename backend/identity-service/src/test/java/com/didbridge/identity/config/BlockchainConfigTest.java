package com.didbridge.identity.config;

import com.didbridge.identity.contract.DidRegistry;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BlockchainConfigTest {

    private static final String VALID_ADDRESS = "0x5FbDB2315678afecb367f032d93F642f64180aa3";
    private static final String VALID_PRIVATE_KEY =
            "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";

    private final BlockchainConfig config = new BlockchainConfig();

    @Test
    void didRegistry_throwsWhenPrivateKeyBlank() {
        assertThatThrownBy(() -> config.didRegistry(mock(Web3j.class), VALID_ADDRESS, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blockchain.account-private-key must be set");
    }

    @Test
    void didRegistry_throwsWhenContractAddressIsInvalid() {
        assertThatThrownBy(() -> config.didRegistry(mock(Web3j.class), "0x1234", VALID_PRIVATE_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blockchain.contract-address must be a valid Ethereum address");
    }

    @Test
    void didRegistry_throwsWhenContractAddressIsZeroAddress() {
        assertThatThrownBy(() -> config.didRegistry(
                mock(Web3j.class),
                "0x0000000000000000000000000000000000000000",
                VALID_PRIVATE_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blockchain.contract-address must be a valid Ethereum address");
    }

    @Test
    void didRegistry_returnsLoadedContract_whenInputsAreValid() {
        DidRegistry registry = config.didRegistry(mock(Web3j.class), VALID_ADDRESS, VALID_PRIVATE_KEY);

        assertThat(registry).isNotNull();
    }
}
