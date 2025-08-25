package com.example.hackathon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 설정을 관리하는 Configuration 클래스
 * 
 * 주요 기능:
 * - 서울시 OpenAPI 호출을 위한 WebClient 빈 설정
 * - 기본 Accept 헤더를 JSON으로 설정
 * - 메모리 버퍼 크기 설정 (2MB)
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Configuration
public class WebClientConfig {
    
    /**
     * 서울시 OpenAPI 호출을 위한 WebClient 빈 생성
     * 
     * @return 설정된 WebClient 인스턴스
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl("http://openapi.seoul.go.kr:8088")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }
}
