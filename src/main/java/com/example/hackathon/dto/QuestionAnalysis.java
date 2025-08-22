package com.example.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 질문 분석 결과를 담는 DTO 클래스
 * GPT API를 통해 사용자 질문에서 추출한 키워드들을 저장
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnalysis {
    
    /**
     * 추출된 지역 정보 (예: "강남구", "서초구")
     */
    private String region;
    
    /**
     * 추출된 날짜 정보 (예: "이번 주말", "다음 주")
     */
    private String date;
    
    /**
     * 추출된 행사 종류 (예: "축제", "공연", "문화")
     */
    private String type;
    
    /**
     * 추출된 행사명 (예: "서울장미축제")
     */
    private String eventName;
}
