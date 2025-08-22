package com.example.hackathon.service;

import com.example.hackathon.config.OpenAIConfig;
import com.example.hackathon.entity.Festival;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 축제 데이터에 대한 AI 요약 및 태그 생성을 담당하는 서비스 클래스
 * 
 * 주요 기능:
 * - OpenAI GPT API를 활용한 축제 정보 요약 생성
 * - 분위기 및 활동 태그 자동 생성
 * - 재시도 로직을 통한 안정적인 API 호출
 * - JSON 형식의 구조화된 응답 처리
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FestivalAIService {
    
    private final OpenAIConfig openAIConfig;
    private final ObjectMapper objectMapper;
    
    private OpenAiService openAiService;
    
    /**
     * 축제 정보를 기반으로 AI 요약을 생성
     * 
     * @param festival 요약을 생성할 축제 정보
     * @return 구조화된 JSON 형태의 요약 정보
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String generateAISummary(Festival festival) {
        log.info("AI 요약 생성 시작: {}", festival.getName());
        
        try {
            String prompt = createSummaryPrompt(festival);
            String response = callOpenAI(prompt);
            return parseSummaryResponse(response);
            
        } catch (Exception e) {
            log.error("AI 요약 생성 실패: {} - {}", festival.getName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 축제 정보를 기반으로 분위기 태그를 생성
     * 
     * @param festival 태그를 생성할 축제 정보
     * @return JSON 배열 형태의 분위기 태그
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String generateMoodTags(Festival festival) {
        log.info("분위기 태그 생성 시작: {}", festival.getName());
        
        try {
            String prompt = createTagsPrompt(festival);
            String response = callOpenAI(prompt);
            return parseTagsResponse(response);
            
        } catch (Exception e) {
            log.error("분위기 태그 생성 실패: {} - {}", festival.getName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 축제 정보를 기반으로 AI 요약 프롬프트를 생성
     * 
     * @param festival 프롬프트를 생성할 축제 정보
     * @return 구조화된 요약 생성을 위한 프롬프트
     */
    private String createSummaryPrompt(Festival festival) {
        return String.format("""
            다음 축제 정보를 바탕으로 구조화된 요약을 JSON 형식으로 생성해주세요.
            
            축제 정보:
            - 이름: %s
            - 날짜: %s
            - 지역: %s
            - 장소: %s
            - 링크: %s
            
            요구사항:
            1. 다음 JSON 구조로 답변해주세요:
            {
                "장소": "구체적인 장소명",
                "날짜": "기간 정보",
                "주요키워드": "축제의 핵심 특징을 나타내는 키워드들 (쉼표로 구분)"
            }
            
            2. 장소는 구체적이고 명확하게 작성해주세요.
            3. 날짜는 한국어로 읽기 쉽게 작성해주세요.
            4. 주요키워드는 3-5개 정도로 핵심적인 것만 추출해주세요.
            
            JSON 형식으로만 답변해주세요.
            """, 
            festival.getName(),
            festival.getDate(),
            festival.getDistrict(),
            festival.getPlace(),
            festival.getLink() != null ? festival.getLink() : "정보 없음"
        );
    }
    
    /**
     * 축제 정보를 기반으로 태그 생성 프롬프트를 생성합니다.
     */
    private String createTagsPrompt(Festival festival) {
        return String.format("""
            다음 축제 정보를 바탕으로 분위기 및 활동 태그를 JSON 배열 형식으로 생성해주세요.
            
            축제 정보:
            - 이름: %s
            - 날짜: %s
            - 지역: %s
            - 장소: %s
            
            요구사항:
            1. 다음 JSON 배열 형식으로 답변해주세요:
            ["태그1", "태그2", "태그3", "태그4", "태그5"]
            
            2. 태그는 다음 카테고리를 포함해주세요:
               - 활동 관련: 문화, 공연, 음식, 체험, 전시, 워크샵 등
               - 분위기 관련: 가족, 야외, 실내, 무료, 유료, 친환경 등
               - 계절/시간 관련: 봄, 여름, 가을, 겨울, 주말, 평일 등
            
            3. 최대 5개의 태그만 생성해주세요.
            4. 축제의 특성을 잘 나타내는 핵심 태그만 선택해주세요.
            
            JSON 배열 형식으로만 답변해주세요.
            """,
            festival.getName(),
            festival.getDate(),
            festival.getDistrict(),
            festival.getPlace()
        );
    }
    
    /**
     * OpenAI API를 호출합니다.
     */
    public String callOpenAI(String prompt, int maxTokens) {
        if (openAiService == null) {
            String apiKey = openAIConfig.getApiKey();
            
            // API 키가 설정되지 않은 경우 예외 발생
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.error("❌ OpenAI API 키가 설정되지 않았습니다!");
                log.error("환경변수 OPENAI_API_KEY 또는 application-local.properties에서 API 키를 설정해주세요.");
                throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다. 환경변수 OPENAI_API_KEY를 설정하거나 application-local.properties 파일에 API 키를 추가해주세요.");
            }
            
            openAiService = new OpenAiService(apiKey);
        }
        
        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(openAIConfig.getModel())
                    .messages(List.of(new ChatMessage("user", prompt)))
                    .maxTokens(maxTokens)
                    .temperature(openAIConfig.getTemperature())
                    .build();
            
            String response = openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
            
            log.info("OpenAI API 호출 성공: {}", response.substring(0, Math.min(50, response.length())) + "...");
            return response;
            
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("OpenAI API 호출 실패", e);
        }
    }
    
    /**
     * OpenAI API를 호출합니다. (기본 maxTokens 사용)
     */
    private String callOpenAI(String prompt) {
        return callOpenAI(prompt, openAIConfig.getMaxTokens());
    }
    
    /**
     * AI 요약 응답을 파싱합니다.
     */
    private String parseSummaryResponse(String response) {
        try {
            // JSON 형식 검증
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 필수 필드 확인
            if (jsonNode.has("장소") && jsonNode.has("날짜") && jsonNode.has("주요키워드")) {
                return response;
            } else {
                log.warn("AI 응답에 필수 필드가 없습니다: {}", response);
                return null;
            }
            
        } catch (JsonProcessingException e) {
            log.error("AI 요약 응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 태그 응답을 파싱합니다.
     */
    private String parseTagsResponse(String response) {
        try {
            // JSON 배열 형식 검증
            JsonNode jsonNode = objectMapper.readTree(response);
            
            if (jsonNode.isArray() && jsonNode.size() <= 5) {
                return response;
            } else {
                log.warn("AI 태그 응답이 올바른 형식이 아닙니다: {}", response);
                return null;
            }
            
        } catch (JsonProcessingException e) {
            log.error("AI 태그 응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
