package com.example.hackathon.service;

import com.example.hackathon.dto.ChatbotRequest;
import com.example.hackathon.dto.ChatbotResponse;
import com.example.hackathon.dto.QuestionAnalysis;
import com.example.hackathon.entity.Festival;
import com.example.hackathon.repository.FestivalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 챗봇 서비스 클래스
 * 
 * 주요 기능:
 * - 사용자 질문 분석 (GPT API 활용)
 * - 문화행사 데이터 검색 및 필터링
 * - 자연어 답변 생성 (GPT API 활용)
 * - 관련 행사 정보 제공
 * - 오류 처리 및 예외 상황 대응
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {
    
    private final FestivalAIService festivalAIService;
    private final FestivalRepository festivalRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 사용자 질문을 처리하여 답변과 관련 행사 정보를 생성
     * 
     * 처리 과정:
     * 1. GPT를 통한 질문 분석 (지역, 날짜, 행사 종류, 행사명 추출)
     * 2. 추출된 키워드를 기반으로 문화행사 검색
     * 3. 검색 결과를 바탕으로 자연어 답변 생성
     * 4. 관련 행사 정보 목록 생성
     * 
     * @param request 사용자 질문 요청
     * @return 챗봇 답변 및 관련 행사 정보
     */
    public ChatbotResponse processQuestion(ChatbotRequest request) {
        try {
            log.info("챗봇 질문 처리 시작: {}", request.question());
            
            // 1단계: 질문 분석 (GPT)
            QuestionAnalysis analysis = analyzeQuestion(request.question());
            log.info("질문 분석 결과: region={}, date={}, type={}, eventName={}", 
                    analysis.region(), analysis.date(), analysis.type(), analysis.eventName());
            
            // 2단계: 데이터 검색
            List<Festival> festivals = searchFestivals(analysis);
            log.info("검색된 축제 개수: {}", festivals.size());
            
            // 검색된 축제 정보 로깅 (디버깅용)
            if (!festivals.isEmpty()) {
                log.info("검색된 축제 예시: {}", 
                    festivals.stream().limit(3).map(f -> f.getName() + "(" + f.getMoodTags() + ")").collect(Collectors.joining(", ")));
            }
            
            // 3단계: 답변 생성 (GPT)
            String answer = generateAnswer(festivals, request.question());
            
            // 4단계: 관련 이벤트 목록 생성
            List<ChatbotResponse.RelatedEvent> relatedEvents = festivals.stream()
                    .limit(5) // 최대 5개로 제한
                    .map(this::convertToRelatedEvent)
                    .collect(Collectors.toList());
            
            return new ChatbotResponse(answer, relatedEvents);
            
        } catch (Exception e) {
            log.error("챗봇 처리 중 오류 발생", e);
            return new ChatbotResponse(
                "죄송합니다. 잠시 후 다시 시도해 주세요.",
                List.of()
            );
        }
    }
    
    /**
     * GPT를 사용하여 사용자 질문에서 키워드 추출
     * 
     * @param question 사용자 질문
     * @return 추출된 키워드 정보
     */
    private QuestionAnalysis analyzeQuestion(String question) {
        try {
            String prompt = String.format("""
                다음은 사용자의 질문입니다. 이 질문에서 '지역', '날짜', '행사 종류', '행사명'을 추출하여 JSON 객체로 반환해 주세요. 
                해당하는 키워드가 없으면 null로 표시하세요.
                
                사용자 질문: "%s"
                
                주의사항:
                1. 행사 종류는 단일 키워드로 추출하세요 (예: "문화 공연" -> "문화" 또는 "공연")
                2. 지역은 "구" 단위로 추출하세요 (예: "강남구", "서초구")
                3. 날짜는 "이번 주말", "다음 주" 등으로 추출하세요
                4. JSON 형식으로만 응답해주세요.
                
                예시:
                질문: "이번 주말 강남구에서 열리는 축제 알려줘"
                응답: {"region": "강남구", "date": "이번 주말", "type": "축제", "eventName": null}
                
                질문: "문화 공연 찾아줘"
                응답: {"region": null, "date": null, "type": "문화", "eventName": null}
                
                JSON 형식으로만 응답해주세요.
                """, question);
            
            String response = festivalAIService.callOpenAI(prompt, 150);
            log.info("GPT 분석 응답 원본: {}", response);
            
            // JSON 응답에서 실제 JSON 부분만 추출
            String jsonResponse = extractJsonFromResponse(response);
            log.info("추출된 JSON: {}", jsonResponse);
            
            QuestionAnalysis analysis = objectMapper.readValue(jsonResponse, QuestionAnalysis.class);
            log.info("파싱된 분석 결과: region={}, date={}, type={}, eventName={}", 
                    analysis.region(), analysis.date(), analysis.type(), analysis.eventName());
            
            return analysis;
            
        } catch (Exception e) {
            log.error("질문 분석 중 오류", e);
            return new QuestionAnalysis(null, null, null, null);
        }
    }
    
    private List<Festival> searchFestivals(QuestionAnalysis analysis) {
        List<Festival> festivals = List.of();
        
        try {
            log.info("검색 시작 - region: {}, type: {}, eventName: {}", 
                    analysis.region(), analysis.type(), analysis.eventName());
            
            // 검색 조건이 없는 경우 기본 데이터 반환
            if (analysis.isEmpty()) {
                log.info("검색 조건이 없음. 기본 데이터 반환");
                return festivalRepository.findAll().stream().limit(10).collect(Collectors.toList());
            }
            
            // 1. 지역 검색 (가장 우선순위)
            if (analysis.region() != null && !analysis.region().trim().isEmpty()) {
                log.info("지역 검색 시도: {}", analysis.region());
                festivals = festivalRepository.findByDistrict(analysis.region());
                log.info("지역 검색 결과: {} ({}개)", analysis.region(), festivals.size());
                
                // 검색 결과가 없으면 부분 매칭 시도
                if (festivals.isEmpty()) {
                    log.info("정확한 지역 매칭 실패. 부분 매칭 시도");
                    List<Festival> allFestivals = festivalRepository.findAll();
                    festivals = allFestivals.stream()
                            .filter(f -> f.getDistrict() != null && 
                                       f.getDistrict().contains(analysis.region().replace("구", "")))
                            .collect(Collectors.toList());
                    log.info("부분 매칭 결과: {}개", festivals.size());
                }
            }
            
            // 2. 행사명 검색
            if (festivals.isEmpty() && analysis.eventName() != null && !analysis.eventName().trim().isEmpty()) {
                log.info("행사명 검색 시도: {}", analysis.eventName());
                festivals = festivalRepository.findByNameContaining(analysis.eventName());
                log.info("행사명 검색 결과: {} ({}개)", analysis.eventName(), festivals.size());
            }
            
            // 3. 키워드 검색
            if (festivals.isEmpty() && analysis.type() != null && !analysis.type().trim().isEmpty()) {
                log.info("키워드 검색 시도: {}", analysis.type());
                festivals = festivalRepository.findByMoodTagsContaining(analysis.type());
                log.info("키워드 검색 결과: {} ({}개)", analysis.type(), festivals.size());
                
                // 키워드 검색 결과가 없으면 행사명으로 검색
                if (festivals.isEmpty()) {
                    festivals = festivalRepository.findByNameContaining(analysis.type());
                    log.info("행사명 키워드 검색 결과: {} ({}개)", analysis.type(), festivals.size());
                }
            }
            
            // 4. 모든 검색이 실패하면 최근 데이터 반환
            if (festivals.isEmpty()) {
                log.warn("모든 검색이 실패. 최근 데이터 반환");
                festivals = festivalRepository.findAll().stream().limit(5).collect(Collectors.toList());
                log.info("최근 데이터 반환: {}개", festivals.size());
            }
            
        } catch (Exception e) {
            log.error("축제 검색 중 오류", e);
            // 오류 발생 시 기본 데이터 반환
            festivals = festivalRepository.findAll().stream().limit(3).collect(Collectors.toList());
        }
        
        return festivals;
    }
    
    private String generateAnswer(List<Festival> festivals, String originalQuestion) {
        try {
            if (festivals.isEmpty()) {
                return "죄송합니다. 찾으시는 행사가 없습니다. 다른 키워드로 검색해보세요.";
            }
            
            String eventsData = festivals.stream()
                    .limit(5)
                    .map(festival -> String.format(
                        "{ \"name\": \"%s\", \"district\": \"%s\", \"date\": \"%s\" }",
                        festival.getName() != null ? festival.getName().replace("\"", "\\\"") : "제목 없음",
                        festival.getDistrict() != null ? festival.getDistrict() : "지역 정보 없음",
                        festival.getDate() != null ? festival.getDate() : "날짜 정보 없음"
                    ))
                    .collect(Collectors.joining(", "));
            
            String prompt = String.format("""
                다음은 사용자가 찾고 있는 행사 데이터 목록입니다. 
                이 데이터를 기반으로 사용자에게 친절하고 자연스러운 문장으로 답변해 주세요.
                
                원본 질문: "%s"
                
                데이터:
                {
                  "events": [%s]
                }
                
                답변 요구사항:
                1. 간결하고 친근한 톤으로 답변 (100자 이내)
                2. 찾은 행사 개수와 주요 행사명 언급
                3. 한국어로 자연스럽게 답변
                
                예시 답변:
                "찾으신 행사가 %d개 있습니다. 주요 행사로는 [행사명] 등이 있어요."
                """, originalQuestion, eventsData, festivals.size());
            
            String answer = festivalAIService.callOpenAI(prompt, 200);
            log.debug("GPT 답변 생성: {}", answer);
            
            return answer.trim();
            
        } catch (Exception e) {
            log.error("답변 생성 중 오류", e);
            if (festivals.isEmpty()) {
                return "죄송합니다. 찾으시는 행사가 없습니다.";
            } else {
                return String.format("찾으신 행사가 %d개 있습니다. 자세한 정보는 아래 목록을 확인해주세요.", festivals.size());
            }
        }
    }
    
    private ChatbotResponse.RelatedEvent convertToRelatedEvent(Festival festival) {
        return new ChatbotResponse.RelatedEvent(
            festival.getId(),
            festival.getName() != null ? festival.getName() : "제목 없음",
            festival.getDistrict() != null ? festival.getDistrict() : "지역 정보 없음",
            festival.getDate() != null ? festival.getDate() : "날짜 정보 없음"
        );
    }
    
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }
        
        // GPT 응답에서 JSON 부분만 추출
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start != -1 && end != -1 && end > start) {
            String json = response.substring(start, end + 1);
            log.debug("추출된 JSON: {}", json);
            return json;
        }
        
        // JSON이 없는 경우 기본 객체 반환
        log.warn("JSON을 찾을 수 없음. 기본 객체 반환. 원본 응답: {}", response);
        return "{\"region\": null, \"date\": null, \"type\": null, \"eventName\": null}";
    }
}
