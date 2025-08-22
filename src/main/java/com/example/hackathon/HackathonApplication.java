package com.example.hackathon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 해커톤 프로젝트 메인 애플리케이션 클래스
 * 
 * 주요 기능:
 * - Spring Boot 애플리케이션 시작점
 * - JPA Auditing 활성화 (생성일시, 수정일시 자동 관리)
 * - 스케줄링 기능 활성화 (정기적인 데이터 수집)
 * - 설정 프로퍼티 활성화
 * - Jackson JSON 직렬화 설정 (LocalDateTime 지원)
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties
public class HackathonApplication {

    /**
     * 애플리케이션 메인 메서드
     * 
     * @param args 명령행 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(HackathonApplication.class, args);
    }
    
    /**
     * Jackson ObjectMapper 커스터마이징
     * LocalDateTime 등의 Java 8 시간 타입을 JSON으로 직렬화할 수 있도록 설정
     * 
     * @return Jackson2ObjectMapperBuilderCustomizer
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization() {
        return builder -> {
            builder.modules(new JavaTimeModule());
        };
    }
}
