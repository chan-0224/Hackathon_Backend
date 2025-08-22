package com.example.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 챗봇 질문 요청을 위한 DTO 클래스
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {
    
    /**
     * 사용자가 입력한 질문
     */
    private String question;
}
