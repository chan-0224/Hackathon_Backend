package com.example.hackathon.dto;

/**
 * 사용자 질문 분석 결과를 담는 DTO 클래스
 * GPT API를 통해 사용자 질문에서 추출한 키워드들을 저장
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
public record QuestionAnalysis(
    /**
     * 추출된 지역 정보 (예: "강남구", "서초구")
     */
    String region,
    
    /**
     * 추출된 날짜 정보 (예: "이번 주말", "다음 주")
     */
    String date,
    
    /**
     * 추출된 행사 종류 (예: "축제", "공연", "문화")
     */
    String type,
    
    /**
     * 추출된 행사명 (예: "서울장미축제")
     */
    String eventName
) {
    /**
     * 모든 필드가 null인지 확인
     */
    public boolean isEmpty() {
        return region == null && date == null && type == null && eventName == null;
    }
    
    /**
     * 유효한 검색 조건이 있는지 확인
     */
    public boolean hasValidSearchCriteria() {
        return region != null || type != null || eventName != null;
    }
}
