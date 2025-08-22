package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 문화행사(축제) 정보를 저장하는 엔티티 클래스
 * 
 * 주요 기능:
 * - 서울시 문화행사 API에서 수집한 데이터 저장
 * - AI가 생성한 요약 정보 및 태그 저장
 * - 생성일시, 수정일시 자동 관리
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Entity
@Table(name = "festivals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Festival {
    
    /**
     * 고유 식별자 (자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 행사명
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * 행사 기간
     */
    @Column(nullable = false)
    private String date;
    
    /**
     * 행사 지역 (구 단위)
     */
    @Column(nullable = false)
    private String district;
    
    /**
     * 행사 장소
     */
    @Column(nullable = false)
    private String place;
    
    /**
     * 행사 관련 링크 (최대 1000자)
     */
    @Column(length = 1000)
    private String link;
    
    /**
     * 중복 방지를 위한 고유 키 (행사명 + 날짜 + 장소 조합)
     */
    @Column(unique = true, length = 500)
    private String uniqueKey;
    
    /**
     * AI가 생성한 축제 요약 정보 (JSON 형식)
     * 예: {"장소": "서울광장", "날짜": "2024년 5월 1일~3일", "주요키워드": "봄맞이, 꽃축제, 문화공연"}
     */
    @Column(columnDefinition = "TEXT")
    private String aiSummary;
    
    /**
     * AI가 생성한 분위기 및 활동 태그 (JSON 배열)
     * 예: ["문화", "공연", "음식", "체험", "가족"]
     */
    @Column(columnDefinition = "JSON")
    private String moodTags;
    
    /**
     * 데이터 생성일시 (자동 설정)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 데이터 수정일시 (자동 설정)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 엔티티 저장 전 실행되는 메서드
     * 생성일시와 수정일시를 현재 시간으로 설정
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 엔티티 수정 전 실행되는 메서드
     * 수정일시를 현재 시간으로 설정
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
