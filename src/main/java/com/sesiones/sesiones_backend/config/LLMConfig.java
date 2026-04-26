package com.sesiones.sesiones_backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LLMProperties.class)
public class LLMConfig {

    @Bean
    public RestClient deepSeekRestClient(RestClient.Builder builder, LLMProperties llmProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(llmProperties.getTimeoutSeconds());
        int timeoutMillis = (int) timeout.toMillis();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);

        return builder
            .baseUrl(llmProperties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }
}
