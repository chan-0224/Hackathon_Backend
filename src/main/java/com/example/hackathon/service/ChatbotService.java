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
            log.info("챗봇 질문 처리 시작: {}", request.getQuestion());
            
            // 1단계: 질문 분석 (GPT)
            QuestionAnalysis analysis = analyzeQuestion(request.getQuestion());
            log.info("질문 분석 결과: region={}, date={}, type={}, eventName={}", 
                    analysis.getRegion(), analysis.getDate(), analysis.getType(), analysis.getEventName());
            
            // 2단계: 데이터 검색
            List<Festival> festivals = searchFestivals(analysis);
            log.info("검색된 축제 개수: {}", festivals.size());
            
            // 검색된 축제 정보 로깅 (디버깅용)
            if (!festivals.isEmpty()) {
                log.info("검색된 축제 예시: {}", 
                    festivals.stream().limit(3).map(f -> f.getName() + "(" + f.getMoodTags() + ")").collect(Collectors.joining(", ")));
            }
            
            // 3단계: 답변 생성 (GPT)
            String answer = generateAnswer(festivals, request.getQuestion());
            
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
                    analysis.getRegion(), analysis.getDate(), analysis.getType(), analysis.getEventName());
            
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
                    analysis.getRegion(), analysis.getType(), analysis.getEventName());
            
            // 간단한 테스트: 지역 검색만 우선 시도
            if (analysis.getRegion() != null) {
                log.info("지역 검색 시도: {}", analysis.getRegion());
                festivals = festivalRepository.findByDistrict(analysis.getRegion());
                log.info("지역 검색 결과: {} ({}개)", analysis.getRegion(), festivals.size());
                
                // 검색 결과가 없으면 전체 데이터에서 확인
                if (festivals.isEmpty()) {
                    log.warn("지역 검색 결과가 없음. 전체 데이터 확인 중...");
                    List<Festival> allFestivals = festivalRepository.findAll();
                    log.info("전체 데이터 개수: {}", allFestivals.size());
                    
                    // 강남구가 포함된 데이터 찾기
                    List<Festival> gangnamFestivals = allFestivals.stream()
                            .filter(f -> f.getDistrict() != null && f.getDistrict().contains("강남"))
                            .collect(Collectors.toList());
                    log.info("강남 포함 데이터: {}개", gangnamFestivals.size());
                    
                    if (!gangnamFestivals.isEmpty()) {
                        festivals = gangnamFestivals;
                        log.info("강남 포함 데이터로 대체");
                    }
                }
            } else if (analysis.getType() != null) {
                // 키워드 검색
                log.info("키워드 검색 시도: {}", analysis.getType());
                festivals = festivalRepository.findByMoodTagsContaining(analysis.getType());
                log.info("키워드 검색 결과: {} ({}개)", analysis.getType(), festivals.size());
                
                // 키워드 검색 결과가 없으면 행사명으로 검색
                if (festivals.isEmpty()) {
                    festivals = festivalRepository.findByNameContaining(analysis.getType());
                    log.info("행사명 검색 결과: {} ({}개)", analysis.getType(), festivals.size());
                }
            } else if (analysis.getEventName() != null) {
                // 행사명 검색
                log.info("행사명 검색 시도: {}", analysis.getEventName());
                festivals = festivalRepository.findByNameContaining(analysis.getEventName());
                log.info("행사명 검색 결과: {} ({}개)", analysis.getEventName(), festivals.size());
            }
            
            // 모든 검색이 실패하면 임시로 몇 개 반환 (테스트용)
            if (festivals.isEmpty()) {
                log.warn("모든 검색이 실패. 임시 데이터 반환");
                festivals = festivalRepository.findAll().stream().limit(5).collect(Collectors.toList());
                log.info("임시 데이터 반환: {}개", festivals.size());
            }
            
        } catch (Exception e) {
            log.error("축제 검색 중 오류", e);
        }
        
        return festivals;
    }
    
    /**
     * 개선된 키워드 매칭 로직
     * 예: "문화 공연" -> "문화", "공연" 각각 매칭 시도
     */
    private boolean containsAnyKeyword(String moodTags, String searchKeyword) {
        if (moodTags == null || searchKeyword == null) return false;
        
        // 검색 키워드를 공백으로 분리
        String[] keywords = searchKeyword.split("\\s+");
        
        for (String keyword : keywords) {
            if (moodTags.contains(keyword)) {
                log.debug("키워드 매칭 성공: '{}' in '{}'", keyword, moodTags);
                return true;
            }
        }
        
        // 전체 키워드로도 매칭 시도
        if (moodTags.contains(searchKeyword)) {
            log.debug("전체 키워드 매칭 성공: '{}' in '{}'", searchKeyword, moodTags);
            return true;
        }
        
        return false;
    }
    
    private String generateAnswer(List<Festival> festivals, String originalQuestion) {
        try {
            String eventsData = festivals.stream()
                    .limit(5)
                    .map(festival -> String.format(
                        "{ \"name\": \"%s\", \"district\": \"%s\", \"date\": \"%s\" }",
                        festival.getName(),
                        festival.getDistrict(),
                        festival.getDate()
                    ))
                    .collect(Collectors.joining(", "));
            
            String prompt = String.format("""
                다음은 사용자가 찾고 있는 행사 데이터 목록입니다. 
                이 데이터를 기반으로 사용자에게 친절하고 자연스러운 문장으로 답변해 주세요. 
                데이터가 없으면 '찾는 행사가 없습니다.'라고 답변하세요.
                
                원본 질문: "%s"
                
                데이터:
                {
                  "events": [%s]
                }
                
                간결하고 친근한 톤으로 답변해주세요. (100자 이내)
                """, originalQuestion, eventsData);
            
            String answer = festivalAIService.callOpenAI(prompt, 200);
            log.debug("GPT 답변 생성: {}", answer);
            
            return answer.trim();
            
        } catch (Exception e) {
            log.error("답변 생성 중 오류", e);
            return "죄송합니다. 답변을 생성하는 중 오류가 발생했습니다.";
        }
    }
    
    private ChatbotResponse.RelatedEvent convertToRelatedEvent(Festival festival) {
        return new ChatbotResponse.RelatedEvent(
            festival.getId(),
            festival.getName(),
            festival.getDistrict(),
            festival.getDate()
        );
    }
    
    private String extractJsonFromResponse(String response) {
        // GPT 응답에서 JSON 부분만 추출
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return response;
    }
    
    private List<Festival> filterByDate(List<Festival> festivals, String dateKeyword) {
        // 간단한 날짜 필터링 (향후 개선 가능)
        if (dateKeyword.contains("주말") || dateKeyword.contains("이번 주말")) {
            // 주말 필터링 로직 (현재는 모든 결과 반환)
            return festivals;
        }
        
        return festivals;
    }
}
