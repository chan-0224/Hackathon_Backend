package com.example.hackathon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정 클래스
 * 
 * 주요 기능:
 * - CORS 설정 (Cross-Origin Resource Sharing)
 * - 프론트엔드와 백엔드 간 통신 허용
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * CORS 설정
     * 
     * 프론트엔드에서 백엔드로의 API 요청을 허용하여 AI 기능들이 정상 작동하도록 합니다.
     * 
     * @param registry CORS 레지스트리
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    // 개발 환경
                    "http://localhost:3000",      // React 개발 서버
                    "http://localhost:5173",      // Vite 개발 서버
                    "http://127.0.0.1:3000",     // React 대체 주소
                    "http://127.0.0.1:5173",     // Vite 대체 주소
                    
                    // 배포 환경 (실제 도메인으로 변경 필요)
                    "http://3.38.210.80",        // EC2 서버 IP
                    "http://3.38.210.80:3000",   // 프론트엔드 포트
                    "http://3.38.210.80:5173",   // Vite 포트
                    
                    // 모든 도메인 허용 (개발 중에만 사용)
                    "*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(false) // "*" 사용 시 false로 설정
                .maxAge(3600); // 1시간
    }
}
