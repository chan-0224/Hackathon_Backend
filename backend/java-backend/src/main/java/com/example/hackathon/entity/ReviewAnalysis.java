package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * 리뷰 분석 결과를 저장하는 엔티티 클래스
 * 
 * 주요 기능:
 * - OpenAI API를 통해 분석된 감정, 점수, 키워드 등 저장
 * - Review 엔티티와 1:N 관계 설정
 * - 분석 성공/실패 상태 및 오류 정보 관리
 * - 생성일시, 수정일시 자동 관리
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Entity
@Table(name = "review_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ReviewAnalysis {
    
    /**
     * 고유 식별자 (자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 분석 대상 리뷰 (Review 엔티티와 1:N 관계)
     * 통합 분석의 경우 null일 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Review review;
    
    /**
     * 축제 평가 점수 (0-100점)
     */
    @Column(name = "festival_score")
    private Integer festivalScore;
    
    /**
     * 긍정 만족도 (0-100%)
     */
    @Column(name = "positive_percentage")
    private Integer positivePercentage;
    
    /**
     * 부정 만족도 (0-100%)
     */
    @Column(name = "negative_percentage")
    private Integer negativePercentage;
    
    /**
     * 긍정 키워드 (JSON 배열 형태로 저장)
     */
    @Column(name = "positive_keywords", columnDefinition = "TEXT")
    private String positiveKeywords;
    
    /**
     * 부정 키워드 (JSON 배열 형태로 저장)
     */
    @Column(name = "negative_keywords", columnDefinition = "TEXT")
    private String negativeKeywords;
    
    /**
     * AI 종합 평가 (요약 텍스트)
     */
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;
    
    /**
     * 전체 분석 결과 (JSON 형태로 저장)
     */
    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult;
    
    /**
     * 분석 성공 여부
     */
    @Column(name = "is_success")
    private Boolean isSuccess;
    
    /**
     * 오류 메시지 (실패 시)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 분석 완료 시간
     */
    @Column(name = "analyzed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime analyzedAt;
    
    /**
     * 데이터 생성일시 (자동 설정)
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 데이터 수정일시 (자동 설정)
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
