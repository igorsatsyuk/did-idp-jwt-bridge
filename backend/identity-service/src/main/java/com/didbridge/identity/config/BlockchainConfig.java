package com.didbridge.identity.config;

import com.didbridge.identity.contract.DidRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        Credentials credentials = Credentials.create(privateKey);
        return DidRegistry.load(contractAddress, web3j, credentials, new DefaultGasProvider());
    }
}
