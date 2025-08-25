package com.example.hackathon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 크롤링 관련 설정을 관리하는 Configuration 클래스
 * 
 * 주요 기능:
 * - application.properties에서 크롤링 설정값 주입
 * - 크롤링 지연시간, 타임아웃, 재시도 등 설정 관리
 * - User-Agent 로테이션 및 차단 감지 설정
 * - 안전한 크롤링을 위한 제한 설정
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "crawler")
@Data
public class CrawlerConfig {
    
    /**
     * 크롤링 요청 간 최소 지연시간 (초)
     */
    private int minDelaySeconds = 3;
    
    /**
     * 크롤링 요청 간 최대 지연시간 (초)
     */
    private int maxDelaySeconds = 8;
    
    /**
     * 한 번에 크롤링할 최대 리뷰 수
     */
    private int maxReviewsPerRequest = 10;
    
    /**
     * 일일 최대 크롤링 요청 수
     */
    private int maxDailyRequests = 50;
    
    /**
     * 요청 타임아웃 (초)
     */
    private int timeoutSeconds = 20;
    
    /**
     * 최대 재시도 횟수
     */
    private int maxRetries = 2;
    
    /**
     * 재시도 간 지연시간 (초)
     */
    private int retryDelaySeconds = 15;
    
    /**
     * 크롤링 활성화 여부
     */
    private boolean enabled = true;
    
    /**
     * User-Agent 목록 (로테이션용)
     * IP 차단 방지를 위해 다양한 브라우저 User-Agent를 사용
     */
    private List<String> userAgents = List.of(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15"
    );
    
    /**
     * 차단 감지 키워드 (응답에 포함되면 크롤링 중단)
     * IP 차단이나 접근 제한을 감지하기 위한 키워드들
     */
    private List<String> blockDetectionKeywords = List.of(
        "접근이 제한되었습니다",
        "자동화된 요청",
        "차단",
        "access denied"
    );
}
