package com.example.hackathon.controller;

import com.example.hackathon.dto.ChatbotRequest;
import com.example.hackathon.dto.ChatbotResponse;
import com.example.hackathon.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 챗봇 API를 제공하는 Controller 클래스
 * 
 * 주요 기능:
 * - 사용자 질문 수신 및 처리
 * - GPT 기반 자연어 처리 및 답변 생성
 * - 관련 행사 정보 제공
 * - 오류 처리 및 예외 상황 대응
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
    
    private final ChatbotService chatbotService;
    
    /**
     * 챗봇 질문 처리 API
     * 
     * 사용자의 자연어 질문을 받아서 GPT를 통해 분석하고,
     * 관련된 문화행사 정보와 함께 답변을 생성하여 반환합니다.
     * 
     * @param request 사용자 질문 정보
     * @return 챗봇 답변 및 관련 행사 정보
     */
    @PostMapping("/ask")
    public ResponseEntity<ChatbotResponse> askQuestion(@RequestBody ChatbotRequest request) {
        log.info("챗봇 질문 수신: {}", request.question());
        
        try {
            ChatbotResponse response = chatbotService.processQuestion(request);
            log.info("챗봇 답변 생성 완료: {}자", response.answer().length());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("챗봇 처리 중 오류 발생", e);
            return ResponseEntity.ok(new ChatbotResponse(
                "죄송합니다. 잠시 후 다시 시도해 주세요.",
                null
            ));
        }
    }
}
