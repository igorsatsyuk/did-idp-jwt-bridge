package com.didbridge.identity.config;

import com.didbridge.identity.contract.DidRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

@Configuration
public class BlockchainConfig {

    @Bean
    public Web3j web3j(@Value("${blockchain.rpc-url}") String rpcUrl) {
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    public DidRegistry didRegistry(
            Web3j web3j,
            @Value("${blockchain.contract-address}") String contractAddress,
            @Value("${blockchain.account-private-key}") String privateKey) {
        if (!StringUtils.hasText(privateKey)) {
            throw new IllegalStateException(
                    "blockchain.account-private-key must be set (e.g. via BLOCKCHAIN_ACCOUNT_PRIVATE_KEY env var)");
        }
        if (!StringUtils.hasText(contractAddress)
                || contractAddress.matches("0x0{40}")) {
            throw new IllegalStateException(
                    "blockchain.contract-address must be set to a valid contract address (e.g. via DID_REGISTRY_ADDRESS env var)");
        }
        Credentials credentials = Credentials.create(privateKey);
        return DidRegistry.load(contractAddress, web3j, credentials, new DefaultGasProvider());
    }
}
