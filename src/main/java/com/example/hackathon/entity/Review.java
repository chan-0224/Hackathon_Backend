package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 축제 리뷰 정보를 저장하는 엔티티 클래스
 * 
 * 주요 기능:
 * - 네이버 블로그에서 크롤링한 축제 관련 리뷰 정보 저장
 * - 크롤링 성공/실패 상태 및 오류 정보 관리
 * - 크롤링 메타데이터 (User-Agent, 요청 시간 등) 저장
 * - 생성일시, 수정일시 자동 관리
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Entity
@Table(name = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    
    /**
     * 고유 식별자 (자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 축제 이름 (검색 키워드)
     */
    @Column(nullable = false, length = 200)
    private String festivalName;
    
    /**
     * 블로그 제목
     */
    @Column(nullable = false, length = 500)
    private String title;
    
    /**
     * 블로그 URL
     */
    @Column(nullable = false, length = 1000)
    private String blogUrl;
    
    /**
     * 블로그 작성자
     */
    @Column(length = 100)
    private String author;
    
    /**
     * 리뷰 본문 내용
     */
    @Column(columnDefinition = "TEXT")
    private String content;
    
    /**
     * 리뷰 작성일 (블로그 포스팅 날짜)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime postDate;
    
    /**
     * 크롤링 성공 여부
     */
    @Column(nullable = false)
    private Boolean isSuccess;
    
    /**
     * 크롤링 시 발생한 오류 메시지
     */
    @Column(length = 1000)
    private String errorMessage;
    
    /**
     * 크롤링 시 사용한 User-Agent
     */
    @Column(length = 500)
    private String userAgent;
    
    /**
     * 크롤링 요청 시간 (IP 차단 방지용 로깅)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime crawledAt;
    
    /**
     * 데이터 생성일시 (자동 설정)
     */
    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 데이터 수정일시 (자동 설정)
     */
    @UpdateTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
