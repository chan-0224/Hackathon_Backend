package com.example.hackathon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAI API 설정을 관리하는 Configuration 클래스
 * 
 * 주요 기능:
 * - application.properties에서 OpenAI API 설정값 주입
 * - API 키, 모델, 토큰 수, 온도 등 설정 관리
 * - 설정 로드 시 유효성 검증 및 로깅
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "openai")
@Slf4j
public class OpenAIConfig {
    
    /**
     * OpenAI API 키
     */
    private String apiKey;
    
    /**
     * 사용할 모델 (기본값: gpt-3.5-turbo)
     */
    private String model = "gpt-3.5-turbo";
    
    /**
     * 최대 토큰 수 (기본값: 300)
     */
    private int maxTokens = 300;
    
    /**
     * 창의성 조절 (0.0 ~ 1.0, 기본값: 0.3)
     */
    private double temperature = 0.3;
    
    /**
     * API 키 반환
     * 
     * @return OpenAI API 키
     */
    public String getApiKey() {
        return apiKey;
    }
    
    /**
     * API 키 설정
     * 
     * @param apiKey OpenAI API 키
     */
    public void setApiKey(String apiKey) {
        log.info("🔧 OpenAIConfig.setApiKey() 호출됨: {}", apiKey != null ? apiKey.substring(0, 20) + "..." : "NULL");
        this.apiKey = apiKey;
    }
    
    /**
     * 모델명 반환
     * 
     * @return 사용할 모델명
     */
    public String getModel() {
        return model;
    }
    
    /**
     * 모델명 설정
     * 
     * @param model 사용할 모델명
     */
    public void setModel(String model) {
        this.model = model;
    }
    
    /**
     * 최대 토큰 수 반환
     * 
     * @return 최대 토큰 수
     */
    public int getMaxTokens() {
        return maxTokens;
    }
    
    /**
     * 최대 토큰 수 설정
     * 
     * @param maxTokens 최대 토큰 수
     */
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    /**
     * 온도값 반환
     * 
     * @return 창의성 조절 온도값
     */
    public double getTemperature() {
        return temperature;
    }
    
    /**
     * 온도값 설정
     * 
     * @param temperature 창의성 조절 온도값
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    /**
     * 설정 로드 확인 및 초기화
     * 애플리케이션 시작 시 설정값들을 로깅하고 유효성을 검증
     */
    @PostConstruct
    public void init() {
        log.info("=== OpenAIConfig 초기화 ===");
        log.info("API Key: {}", apiKey != null ? apiKey.substring(0, 20) + "..." : "NULL");
        log.info("Model: {}", model);
        log.info("Max Tokens: {}", maxTokens);
        log.info("Temperature: {}", temperature);
        log.info("==========================");
        
        // API 키가 없으면 경고
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("⚠️ OpenAI API 키가 설정되지 않았습니다!");
            log.error("application.properties에서 openai.api.key를 확인해주세요.");
        } else {
            log.info("✅ OpenAI API 키가 정상적으로 설정되었습니다.");
        }
    }
}
