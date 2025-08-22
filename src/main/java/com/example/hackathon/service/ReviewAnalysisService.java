package com.example.hackathon.service;

import com.example.hackathon.config.OpenAIConfig;
import com.example.hackathon.entity.Review;
import com.example.hackathon.entity.ReviewAnalysis;
import com.example.hackathon.repository.ReviewAnalysisRepository;
import com.example.hackathon.repository.ReviewRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import jakarta.annotation.PostConstruct;

/**
 * OpenAI API를 사용하여 리뷰를 분석하는 서비스
 * 감정 분석, 점수 평가, 키워드 추출 등을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewAnalysisService {
    
    private final OpenAIConfig openAIConfig;
    private final ReviewAnalysisRepository reviewAnalysisRepository;
    private final ReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;
    
    private OpenAiService openAiService;
    
    /**
     * 서비스 초기화 시 API 키 확인
     */
    @PostConstruct
    public void init() {
        log.info("=== ReviewAnalysisService 초기화 ===");
        log.info("OpenAIConfig: {}", openAIConfig);
        log.info("API Key: {}", openAIConfig.getApiKey());
        log.info("Model: {}", openAIConfig.getModel());
        log.info("Max Tokens: {}", openAIConfig.getMaxTokens());
        log.info("Temperature: {}", openAIConfig.getTemperature());
        log.info("================================");
    }
    
    /**
     * OpenAI 서비스 초기화
     */
    private OpenAiService getOpenAiService() {
        if (openAiService == null) {
            String apiKey = openAIConfig.getApiKey();
            
            log.info("OpenAI API 키 설정: {}", apiKey != null ? apiKey.substring(0, 20) + "..." : "NULL");
            
            // API 키가 설정되지 않은 경우 예외 발생
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.error("❌ OpenAI API 키가 설정되지 않았습니다!");
                log.error("환경변수 OPENAI_API_KEY 또는 application-local.properties에서 API 키를 설정해주세요.");
                throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다. 환경변수 OPENAI_API_KEY를 설정하거나 application-local.properties 파일에 API 키를 추가해주세요.");
            }
            
            openAiService = new OpenAiService(apiKey);
        }
        return openAiService;
    }
    
    /**
     * 단일 리뷰를 분석합니다.
     */
    public ReviewAnalysis analyzeReview(Review review) {
        log.info("리뷰 분석 시작: {}", review.getTitle());
        
        try {
            // 1단계: 관련성 판단
            String relevancePrompt = createRelevancePrompt(review);
            String relevanceResult = callOpenAI(relevancePrompt);
            
            // 관련성 판단 결과 파싱
            boolean isRelevant = parseRelevanceResult(relevanceResult);
            
            if (!isRelevant) {
                log.info("축제와 관련 없는 리뷰 건너뛰기: {}", review.getTitle());
                
                // 관련 없는 리뷰로 표시
                ReviewAnalysis irrelevantAnalysis = ReviewAnalysis.builder()
                        .review(review)
                        .festivalScore(null)
                        .analysisResult("{\"is_relevant\": false, \"reason\": \"축제와 관련 없는 내용\"}")
                        .isSuccess(true)
                        .analyzedAt(LocalDateTime.now())
                        .build();
                
                reviewAnalysisRepository.save(irrelevantAnalysis);
                return irrelevantAnalysis;
            }
            
            // 2단계: 점수 평가 (관련 있는 경우만)
            String scorePrompt = createScorePrompt(review);
            String scoreResult = callOpenAI(scorePrompt);
            
            // 점수 평가 결과 파싱
            ReviewAnalysis analysis = parseScoreResult(review, scoreResult);
            
            // DB 저장
            reviewAnalysisRepository.save(analysis);
            
            log.info("리뷰 분석 완료: {}", review.getTitle());
            return analysis;
            
        } catch (Exception e) {
            log.error("리뷰 분석 실패: {} - {}", review.getTitle(), e.getMessage());
            
            // 실패한 분석 결과 저장
            ReviewAnalysis failedAnalysis = ReviewAnalysis.builder()
                    .review(review)
                    .isSuccess(false)
                    .errorMessage(e.getMessage())
                    .analyzedAt(LocalDateTime.now())
                    .build();
            
            reviewAnalysisRepository.save(failedAnalysis);
            return failedAnalysis;
        }
    }
    
    /**
     * 축제의 모든 리뷰를 분석합니다.
     */
    public List<ReviewAnalysis> analyzeFestivalReviews(String festivalName) {
        log.info("축제 리뷰 분석 시작: {}", festivalName);
        
        // 해당 축제의 성공한 리뷰만 조회 (실제 수집된 개수)
        List<Review> reviews = reviewRepository.findByFestivalNameAndIsSuccessTrue(festivalName);
        
        if (reviews.isEmpty()) {
            log.warn("분석할 리뷰가 없습니다: {}", festivalName);
            return List.of();
        }
        
        log.info("분석할 리뷰 수: {}개 (실제 수집된 개수)", reviews.size());
        
        // 각 리뷰를 개별적으로 분석
        List<ReviewAnalysis> analyses = new ArrayList<>();
        int analyzedCount = 0;
        
        for (Review review : reviews) {
            try {
                // 이미 분석된 리뷰인지 확인
                List<ReviewAnalysis> existingAnalyses = reviewAnalysisRepository.findByReviewIdOrderByCreatedAtDesc(review.getId());
                if (!existingAnalyses.isEmpty() && existingAnalyses.get(0).getIsSuccess()) {
                    log.info("이미 분석된 리뷰 건너뛰기: {}", review.getTitle());
                    analyses.add(existingAnalyses.get(0));
                    continue;
                }
                
                // 리뷰 분석
                ReviewAnalysis analysis = analyzeReview(review);
                analyses.add(analysis);
                analyzedCount++;
                
                log.info("리뷰 분석 진행: {}/{}", analyzedCount, reviews.size());
                
                // API 호출 간격 조절 (비용 절약)
                Thread.sleep(1000); // 1초 대기
                
            } catch (Exception e) {
                log.error("리뷰 분석 중 오류 발생: {} - {}", review.getTitle(), e.getMessage());
            }
        }
        
        log.info("축제 리뷰 분석 완료: {} (성공: {}개 / 총 {}개)", festivalName, 
                analyses.stream().filter(a -> a.getIsSuccess()).count(), reviews.size());
        
        return analyses;
    }
    
    /**
     * 관련성 판단 프롬프트를 생성합니다.
     */
    private String createRelevancePrompt(Review review) {
        return String.format("""
            축제 관련성 판단: 제목=%s, 내용=%s
            
            JSON만 반환: {"is_relevant": true/false, "reason": "판단 이유"}
            기준: 
            - true: 축제 경험, 후기, 평가, 방문기, 체험담 등이 포함된 경우
            - false: 축제와 무관한 내용, 광고, 다른 주제의 글, 단순 정보성 글인 경우
            """, review.getTitle(), review.getContent());
    }
    
    /**
     * 점수 평가 프롬프트를 생성합니다.
     */
    private String createScorePrompt(Review review) {
        return String.format("""
            축제 리뷰 분석: 제목=%s, 내용=%s
            
            JSON만 반환: {"festival_score": 0~100}
            기준: 0-20(매우나쁨), 21-40(나쁨), 41-60(보통), 61-80(좋음), 81-100(매우좋음)
            """, review.getTitle(), review.getContent());
    }
    
    /**
     * OpenAI API를 호출합니다.
     */
    private String callOpenAI(String prompt) {
        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(openAIConfig.getModel())
                    .messages(List.of(new ChatMessage("user", prompt)))
                    .maxTokens(openAIConfig.getMaxTokens())
                    .temperature(openAIConfig.getTemperature())
                    .build();
            
            return getOpenAiService().createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
                    
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage());
        }
    }
    
    /**
     * 관련성 판단 결과를 파싱합니다.
     */
    private boolean parseRelevanceResult(String relevanceResult) {
        try {
            JsonNode jsonNode = objectMapper.readTree(relevanceResult);
            return jsonNode.get("is_relevant").asBoolean();
        } catch (Exception e) {
            log.error("관련성 판단 결과 파싱 실패: {}", e.getMessage());
            // 파싱 실패 시 기본적으로 관련 있다고 가정
            return true;
        }
    }
    
    /**
     * 점수 평가 결과를 파싱하여 ReviewAnalysis 객체로 변환합니다.
     */
    private ReviewAnalysis parseScoreResult(Review review, String scoreResult) {
        try {
            JsonNode jsonNode = objectMapper.readTree(scoreResult);
            
            return ReviewAnalysis.builder()
                    .review(review)
                    .festivalScore(jsonNode.get("festival_score").asInt())
                    .analysisResult(scoreResult)
                    .isSuccess(true)
                    .analyzedAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("점수 평가 결과 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("점수 평가 결과 파싱 실패: " + e.getMessage());
        }
    }
}
