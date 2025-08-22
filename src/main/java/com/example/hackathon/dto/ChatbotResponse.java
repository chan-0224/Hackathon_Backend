package com.example.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

/**
 * 챗봇 응답을 위한 DTO 클래스
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {
    
    /**
     * 챗봇이 생성한 답변
     */
    private String answer;
    
    /**
     * 답변과 관련된 행사 목록
     */
    private List<RelatedEvent> relatedEvents;
    
    /**
     * 관련 행사 정보를 담는 내부 클래스
     * 
     * @author 해커톤 팀
     * @version 1.0
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedEvent {
        
        /**
         * 행사 고유 식별자
         */
        private Long id;
        
        /**
         * 행사명
         */
        private String name;
        
        /**
         * 행사 지역 (구 단위)
         */
        private String district;
        
        /**
         * 행사 기간
         */
        private String date;
    }
}
