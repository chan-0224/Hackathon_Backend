package com.example.hackathon.dto;

import java.util.List;

/**
 * 챗봇 응답을 위한 Record (JDK 21 기능 활용)
 * 
 * @param answer 챗봇이 생성한 답변
 * @param relatedEvents 답변과 관련된 행사 목록
 * @author 해커톤 팀
 * @version 2.0
 */
public record ChatbotResponse(
    String answer,
    List<RelatedEvent> relatedEvents
) {
    /**
     * 관련 행사 정보를 담는 Record
     * 
     * @param id 행사 고유 식별자
     * @param name 행사명
     * @param district 행사 지역 (구 단위)
     * @param date 행사 기간
     * @author 해커톤 팀
     * @version 2.0
     */
    public record RelatedEvent(
        Long id,
        String name,
        String district,
        String date
    ) {}
}
