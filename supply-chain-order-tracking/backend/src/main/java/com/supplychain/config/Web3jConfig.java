package com.supplychain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3jConfig {
    @Bean
    Web3j web3j(@Value("${BLOCKCHAIN_RPC_URL:http://127.0.0.1:8545}") String rpcUrl) {
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    WebMvcConfigurer cors(@Value("${FRONTEND_ORIGIN:http://localhost:5173}") String frontendOrigin) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**").allowedOrigins(frontendOrigin)
                        .allowedMethods("GET", "POST", "PUT", "OPTIONS").allowedHeaders("Authorization", "Content-Type");
            }
        };
    }
}
