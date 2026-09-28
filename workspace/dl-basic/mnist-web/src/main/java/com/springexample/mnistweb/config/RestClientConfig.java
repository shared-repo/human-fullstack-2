package com.springexample.mnistweb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient classifierRestClient(RestClient.Builder builder,
                                           // @Value : application.properties에서 읽기
                                           @Value("${classifier.api.base-url}") String baseUrl) {
        // 모델 추론은 시간이 걸릴 수 있으므로 읽기 타임아웃을 넉넉하게 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(30));

        return builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}