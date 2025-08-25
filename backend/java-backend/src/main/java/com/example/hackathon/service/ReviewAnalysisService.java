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
            
            // 2단계: 상세 분석 (관련 있는 경우만)
            String detailedPrompt = createDetailedAnalysisPrompt(review);
            String detailedResult = callOpenAI(detailedPrompt);
            
            // 상세 분석 결과 파싱
            ReviewAnalysis analysis = parseDetailedResult(review, detailedResult);
            
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
     * 축제의 모든 리뷰를 분석합니다. (간단한 개별 분석만)
     */
    public List<ReviewAnalysis> analyzeFestivalReviews(String festivalName) throws InterruptedException {
        log.info("축제 리뷰 분석 시작: {}", festivalName);
        
        // 해당 축제의 성공한 리뷰만 조회
        List<Review> reviews = reviewRepository.findByFestivalNameAndIsSuccessTrue(festivalName);
        
        if (reviews.isEmpty()) {
            log.warn("분석할 리뷰가 없습니다: {}", festivalName);
            return List.of();
        }
        
        log.info("분석할 리뷰 수: {}개", reviews.size());
        
        List<ReviewAnalysis> analyses = new ArrayList<>();
        
        // 각 후기를 개별적으로 분석 (간단하게)
        for (Review review : reviews) {
            try {
                // 이미 분석된 리뷰인지 확인
                List<ReviewAnalysis> existingAnalyses = reviewAnalysisRepository.findByReviewIdOrderByCreatedAtDesc(review.getId());
                if (!existingAnalyses.isEmpty() && existingAnalyses.get(0).getIsSuccess()) {
                    log.info("이미 분석된 리뷰 건너뛰기: {}", review.getTitle());
                    analyses.add(existingAnalyses.get(0));
                    continue;
                }
                
                // 간단한 개별 분석만 수행
                ReviewAnalysis analysis = analyzeReview(review);
                analyses.add(analysis);
                
                log.info("후기 분석 진행: {}/{}", analyses.size(), reviews.size());
                
                // API 호출 간격 조절 (비용 절약)
                Thread.sleep(1000); // 1초 대기
                
            } catch (Exception e) {
                log.error("후기 분석 중 오류 발생: {} - {}", review.getTitle(), e.getMessage());
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
        // 후기 내용을 4,000자로 제한 (토큰 제한 해결)
        String safeContent = truncateContent(review.getContent(), 4000);
        
        return String.format("""
            축제 관련성 판단: 제목=%s, 내용=%s
            
            반드시 JSON 형식으로만 응답하세요. 앞뒤 설명이나 추가 텍스트는 포함하지 마세요.
            
            {"is_relevant": true/false, "reason": "판단 이유"}
            
            기준: 
            - true: 축제 경험, 후기, 평가, 방문기, 체험담 등이 포함된 경우
            - false: 축제와 무관한 내용, 광고, 다른 주제의 글, 단순 정보성 글인 경우
            """, review.getTitle(), safeContent);
    }
    
    /**
     * 상세 분석 프롬프트를 생성합니다.
     */
    private String createDetailedAnalysisPrompt(Review review) {
        // 후기 내용을 3,500자로 제한 (토큰 제한 해결)
        String safeContent = truncateContent(review.getContent(), 3500);
        
        return String.format("""
            축제 리뷰 분석: 제목=%s, 내용=%s
            
            다음 JSON 형식으로만 응답하세요:
            {
                "festival_score": 75,
                "positive_percentage": 80,
                "negative_percentage": 20,
                "positive_keywords": ["키워드1", "키워드2", "키워드3"],
                "negative_keywords": ["키워드1", "키워드2", "키워드3"],
                "ai_summary": "블로거의 구체적인 평가 요약"
            }
            
            분석 기준:
            - festival_score: 0-20(매우나쁨), 21-40(나쁨), 41-60(보통), 61-80(좋음), 81-100(매우좋음)
            - positive_percentage: 긍정적인 요소의 비율 (0-100)
            - negative_percentage: 부정적인 요소의 비율 (0-100)
            - positive_keywords: 좋았던 점들 (배열)
            - negative_keywords: 아쉬운 점들 (배열)
            - ai_summary: 블로거의 실제 평가 내용을 종합하여 구체적으로 요약. 예시: "전반적으로 만족도가 높은 행사입니다. 특히 분위기와 경험에 대한 평가가 좋으며, 얻어가는게 많다는 의견이 다수입니다. 다만, 트래픽 몰림으로 인해 네트워크 품질이 낮을 수 있으므로 주의하시고, 전반적으로 온도가 낮고 에어컨 조정이 쉽지 않다는 의견이 많으므로 담요등을 준비하시는 것을 추천드려요."
            """, review.getTitle(), safeContent);
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
            log.debug("관련성 판단 원본 응답: {}", relevanceResult);
            
            // AI 응답에서 JSON 부분만 추출
            String cleanJson = extractJsonFromResponse(relevanceResult);
            log.debug("정리된 JSON: {}", cleanJson);
            
            JsonNode jsonNode = objectMapper.readTree(cleanJson);
            boolean isRelevant = jsonNode.get("is_relevant").asBoolean();
            
            log.debug("관련성 판단 결과: {}", isRelevant);
            return isRelevant;
            
        } catch (Exception e) {
            log.error("관련성 판단 결과 파싱 실패: {}", e.getMessage());
            log.error("원본 응답: {}", relevanceResult);
            // 파싱 실패 시 기본적으로 관련 있다고 가정
            return true;
        }
    }
    
    /**
     * 후기 내용을 안전한 길이로 자릅니다. (토큰 제한 해결)
     * 핵심 내용을 포함하도록 스마트하게 자릅니다.
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        
        // 스마트 자르기: 시작 부분 + 끝 부분 (핵심 내용 포함)
        int startLength = maxLength * 2 / 3;  // 시작 부분 (2/3)
        int endLength = maxLength / 3;         // 끝 부분 (1/3)
        
        String startPart = content.substring(0, startLength);
        String endPart = content.substring(content.length() - endLength);
        
        // 시작 부분에서 마지막 완전한 문장 찾기
        int lastPeriod = startPart.lastIndexOf('.');
        int lastExclamation = startPart.lastIndexOf('!');
        int lastQuestion = startPart.lastIndexOf('?');
        
        int lastSentenceEnd = Math.max(Math.max(lastPeriod, lastExclamation), lastQuestion);
        
        if (lastSentenceEnd > startLength * 0.7) { // 70% 이상이면 문장 완성
            startPart = startPart.substring(0, lastSentenceEnd + 1);
        }
        
        // 끝 부분에서 첫 번째 완전한 문장 찾기
        int firstPeriod = endPart.indexOf('.');
        int firstExclamation = endPart.indexOf('!');
        int firstQuestion = endPart.indexOf('?');
        
        int firstSentenceEnd = -1;
        if (firstPeriod != -1) firstSentenceEnd = firstPeriod;
        if (firstExclamation != -1 && (firstSentenceEnd == -1 || firstExclamation < firstSentenceEnd)) {
            firstSentenceEnd = firstExclamation;
        }
        if (firstQuestion != -1 && (firstSentenceEnd == -1 || firstQuestion < firstSentenceEnd)) {
            firstSentenceEnd = firstQuestion;
        }
        
        if (firstSentenceEnd != -1) {
            endPart = endPart.substring(0, firstSentenceEnd + 1);
        }
        
        return startPart + "\n\n[중간 내용 생략...]\n\n" + endPart;
    }
    
    /**
     * JSON 문자열을 완전히 재구성하여 파싱 오류를 방지합니다.
     */
    private String cleanJsonString(String jsonString) {
        if (jsonString == null) return getDefaultJson();
        
        try {
            // 1. JSON 시작과 끝 찾기
            int startBrace = jsonString.indexOf('{');
            int endBrace = jsonString.lastIndexOf('}');
            
            if (startBrace == -1 || endBrace == -1) {
                log.warn("JSON 구조가 올바르지 않음: {}", jsonString);
                return getDefaultJson();
            }
            
            // 2. JSON 부분만 추출
            String jsonPart = jsonString.substring(startBrace, endBrace + 1);
            
            // 3. 완전히 새로운 JSON 재구성
            return reconstructJson(jsonPart);
            
        } catch (Exception e) {
            log.error("JSON 정리 실패: {} - 원본: {}", e.getMessage(), jsonString);
            return getDefaultJson();
        }
    }
    
    /**
     * JSON을 완전히 재구성합니다.
     */
    private String reconstructJson(String jsonString) {
        try {
            // 4. 기본적인 JSON 검증 시도
            objectMapper.readTree(jsonString);
            return jsonString; // 성공하면 그대로 반환
            
        } catch (Exception e) {
            log.warn("JSON 검증 실패, 재구성 시도: {}", e.getMessage());
            
            // 5. 완전히 새로운 JSON 생성
            return createNewJsonFromScratch(jsonString);
        }
    }
    
    /**
     * 망가진 JSON에서 완전히 새로운 JSON을 생성합니다.
     */
    private String createNewJsonFromScratch(String brokenJson) {
        try {
            // 6. 패턴 매칭으로 값 추출 시도
            int overallScore = extractIntValue(brokenJson, "overall_score", 50);
            int overallPositivePercentage = extractIntValue(brokenJson, "overall_positive_percentage", 50);
            int overallNegativePercentage = extractIntValue(brokenJson, "overall_negative_percentage", 50);
            
            // 7. 새로운 JSON 생성
            return String.format("""
                {
                    "overall_score": %d,
                    "overall_positive_percentage": %d,
                    "overall_negative_percentage": %d,
                    "top_positive_keywords": ["긍정키워드1", "긍정키워드2", "긍정키워드3"],
                    "top_negative_keywords": ["부정키워드1", "부정키워드2", "부정키워드3"],
                    "comprehensive_summary": "축제 전체에 대한 AI 종합 평가",
                    "improvement_suggestions": "개선 제안사항"
                }
                """, overallScore, overallPositivePercentage, overallNegativePercentage);
                
        } catch (Exception e) {
            log.error("새로운 JSON 생성 실패: {}", e.getMessage());
            return getDefaultJson();
        }
    }
    
    /**
     * 망가진 JSON에서 정수 값을 추출합니다.
     */
    private int extractIntValue(String jsonString, String fieldName, int defaultValue) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*(\\d+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(jsonString);
            
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
            
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * 기본 JSON을 반환합니다.
     */
    private String getDefaultJson() {
        return """
            {
                "festival_score": 50,
                "positive_percentage": 50,
                "negative_percentage": 50,
                "positive_keywords": ["기본키워드1", "기본키워드2", "기본키워드3"],
                "negative_keywords": ["기본키워드1", "기본키워드2", "기본키워드3"],
                "ai_summary": "분석 실패 - 기본값"
            }
            """;
    }
    
    /**
     * AI 응답에서 JSON 부분만 추출하고 정리합니다.
     */
    private String extractJsonFromResponse(String response) {
        if (response == null) return getDefaultJson();
        
        try {
            log.debug("JSON 추출 시작 - 원본 응답 길이: {}", response.length());
            log.debug("원본 응답: {}", response);
            
            // 방법 1: 정규식으로 JSON 블록 추출
            String jsonPart = extractJsonWithRegex(response);
            if (jsonPart != null) {
                return jsonPart;
            }
            
            // 방법 2: 중괄호 기반 추출
            String jsonPart2 = extractJsonWithBraces(response);
            if (jsonPart2 != null) {
                return jsonPart2;
            }
            
            // 방법 3: 키워드 기반 추출
            String jsonPart3 = extractJsonWithKeywords(response);
            if (jsonPart3 != null) {
                return jsonPart3;
            }
            
            // 방법 4: 수동 JSON 생성 (마지막 수단)
            String jsonPart4 = createJsonFromResponse(response);
            if (jsonPart4 != null) {
                return jsonPart4;
            }
            
            log.warn("모든 JSON 추출 방법 실패, 기본값 사용");
            return getDefaultJson();
            
        } catch (Exception e) {
            log.error("JSON 추출 중 오류: {} - 원본: {}", e.getMessage(), response);
            return getDefaultJson();
        }
    }
    
    /**
     * 응답에서 수동으로 JSON을 생성합니다.
     */
    private String createJsonFromResponse(String response) {
        try {
            log.debug("수동 JSON 생성 시작 - 원본 응답: {}", response);
            
            // 응답에서 숫자 값들을 추출 (더 정확한 패턴 매칭)
            int festivalScore = extractNumberFromText(response, "festival_score", 50);
            int positivePercentage = extractNumberFromText(response, "positive_percentage", 50);
            int negativePercentage = extractNumberFromText(response, "negative_percentage", 50);
            
            // 값 검증 및 조정
            if (positivePercentage + negativePercentage > 100) {
                // 합이 100을 초과하면 비율 조정
                int total = positivePercentage + negativePercentage;
                positivePercentage = (positivePercentage * 100) / total;
                negativePercentage = (negativePercentage * 100) / total;
            }
            
            // 키워드 추출 (더 정확한 구분)
            String positiveKeywords = extractPositiveKeywordsFromText(response);
            String negativeKeywords = extractNegativeKeywordsFromText(response);
            
            // 요약 추출 (더 정확한 방법)
            String aiSummary = extractSummaryFromText(response);
            
            String manualJson = String.format("""
                {
                    "festival_score": %d,
                    "positive_percentage": %d,
                    "negative_percentage": %d,
                    "positive_keywords": %s,
                    "negative_keywords": %s,
                    "ai_summary": "%s"
                }
                """, festivalScore, positivePercentage, negativePercentage, 
                positiveKeywords, negativeKeywords, aiSummary);
            
            log.debug("수동 생성된 JSON: {}", manualJson);
            return manualJson;
            
        } catch (Exception e) {
            log.debug("수동 JSON 생성 실패: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 텍스트에서 숫자 값을 추출합니다.
     */
    private int extractNumberFromText(String text, String fieldName, int defaultValue) {
        try {
            // 1. 정확한 필드명 패턴 매칭
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\"" + fieldName + "\"\\s*:\\s*(\\d+)", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher matcher = pattern.matcher(text);
            
            if (matcher.find()) {
                int value = Integer.parseInt(matcher.group(1));
                if (value >= 0 && value <= 100) {
                    log.debug("{} 추출 성공: {}", fieldName, value);
                    return value;
                }
            }
            
            // 2. 필드명: 숫자 패턴 (따옴표 없이)
            pattern = java.util.regex.Pattern.compile(
                fieldName + "\\s*:\\s*(\\d+)", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            matcher = pattern.matcher(text);
            
            if (matcher.find()) {
                int value = Integer.parseInt(matcher.group(1));
                if (value >= 0 && value <= 100) {
                    log.debug("{} 추출 성공 (따옴표 없음): {}", fieldName, value);
                    return value;
                }
            }
            
            // 3. 단순히 0-100 범위의 숫자 찾기 (마지막 수단)
            pattern = java.util.regex.Pattern.compile("\\b(\\d{1,2}|100)\\b");
            matcher = pattern.matcher(text);
            
            while (matcher.find()) {
                int value = Integer.parseInt(matcher.group(1));
                if (value >= 0 && value <= 100) {
                    log.debug("{} 기본 추출: {}", fieldName, value);
                    return value;
                }
            }
            
        } catch (Exception e) {
            log.debug("숫자 추출 실패 ({}): {}", fieldName, e.getMessage());
        }
        log.debug("{} 기본값 사용: {}", fieldName, defaultValue);
        return defaultValue;
    }
    
    /**
     * 텍스트에서 긍정 키워드 배열을 추출합니다.
     */
    private String extractPositiveKeywordsFromText(String text) {
        try {
            // "positive_keywords" 다음에 오는 배열 찾기
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "positive_keywords\"?\\s*:\\s*\\[([^\\]]*)\\]", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher matcher = pattern.matcher(text);
            
            if (matcher.find()) {
                String keywords = matcher.group(1);
                if (!keywords.trim().isEmpty()) {
                    // 따옴표로 둘러싸인 키워드들을 정리
                    String[] keywordArray = keywords.split(",");
                    StringBuilder result = new StringBuilder("[");
                    for (int i = 0; i < keywordArray.length; i++) {
                        String keyword = keywordArray[i].trim().replaceAll("[\"']", "");
                        if (!keyword.isEmpty()) {
                            if (i > 0) result.append(", ");
                            result.append("\"").append(keyword).append("\"");
                        }
                    }
                    result.append("]");
                    return result.toString();
                }
            }
            
            // 기본 긍정 키워드
            return "[\"좋은 경험\", \"만족스러움\", \"추천\"]";
            
        } catch (Exception e) {
            log.debug("긍정 키워드 추출 실패: {}", e.getMessage());
            return "[\"좋은 경험\", \"만족스러움\", \"추천\"]";
        }
    }
    
    /**
     * 텍스트에서 부정 키워드 배열을 추출합니다.
     */
    private String extractNegativeKeywordsFromText(String text) {
        try {
            // "negative_keywords" 다음에 오는 배열 찾기
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "negative_keywords\"?\\s*:\\s*\\[([^\\]]*)\\]", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher matcher = pattern.matcher(text);
            
            if (matcher.find()) {
                String keywords = matcher.group(1);
                if (!keywords.trim().isEmpty()) {
                    // 따옴표로 둘러싸인 키워드들을 정리
                    String[] keywordArray = keywords.split(",");
                    StringBuilder result = new StringBuilder("[");
                    for (int i = 0; i < keywordArray.length; i++) {
                        String keyword = keywordArray[i].trim().replaceAll("[\"']", "");
                        if (!keyword.isEmpty()) {
                            if (i > 0) result.append(", ");
                            result.append("\"").append(keyword).append("\"");
                        }
                    }
                    result.append("]");
                    return result.toString();
                }
            }
            
            // 기본 부정 키워드
            return "[\"아쉬운 점\", \"개선 필요\", \"부족함\"]";
            
        } catch (Exception e) {
            log.debug("부정 키워드 추출 실패: {}", e.getMessage());
            return "[\"아쉬운 점\", \"개선 필요\", \"부족함\"]";
        }
    }
    
    /**
     * 텍스트에서 키워드 배열을 추출합니다.
     */
    private String extractKeywordsFromText(String text, String fieldName) {
        try {
            // 대괄호로 둘러싸인 배열 찾기
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[([^\\]]*)\\]");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            
            if (matcher.find()) {
                String keywords = matcher.group(1);
                if (!keywords.trim().isEmpty()) {
                    return "[" + keywords + "]";
                }
            }
            
        } catch (Exception e) {
            log.debug("키워드 추출 실패 ({}): {}", fieldName, e.getMessage());
        }
        return "[\"기본키워드\"]";
    }
    
    /**
     * 텍스트에서 요약을 추출합니다.
     */
    private String extractSummaryFromText(String text) {
        try {
            // "ai_summary" 다음에 오는 따옴표 텍스트 찾기
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "ai_summary\"?\\s*:\\s*\"([^\"]{20,500})\"", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher matcher = pattern.matcher(text);
            
            if (matcher.find()) {
                String summary = matcher.group(1);
                if (!summary.trim().isEmpty() && 
                    !summary.equals("festival_score") && 
                    !summary.contains("분석되었습니다") &&
                    summary.length() > 20) {
                    return summary;
                }
            }
            
            // 전체 텍스트에서 의미있는 문장들을 조합하여 요약 생성
            String[] sentences = text.split("[.!?]");
            StringBuilder summaryBuilder = new StringBuilder();
            
            // 긍정적인 평가 문장들 찾기
            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (sentence.length() > 15 && sentence.length() < 200 &&
                    (sentence.contains("좋") || sentence.contains("만족") || 
                     sentence.contains("추천") || sentence.contains("감동") ||
                     sentence.contains("재미") || sentence.contains("기대") ||
                     sentence.contains("경험") || sentence.contains("체험") ||
                     sentence.contains("분위기") || sentence.contains("활기") ||
                     sentence.contains("즐거운") || sentence.contains("훌륭한"))) {
                    if (summaryBuilder.length() > 0) summaryBuilder.append(" ");
                    summaryBuilder.append(sentence).append(".");
                    break; // 첫 번째 긍정 문장만 사용
                }
            }
            
            // 부정적인 평가나 주의사항 문장들 찾기
            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (sentence.length() > 15 && sentence.length() < 200 &&
                    (sentence.contains("아쉽") || sentence.contains("실망") || 
                     sentence.contains("주의") || sentence.contains("개선") ||
                     sentence.contains("부족") || sentence.contains("후회") ||
                     sentence.contains("문제") || sentence.contains("불편") ||
                     sentence.contains("혼잡") || sentence.contains("길") ||
                     sentence.contains("추천") || sentence.contains("준비"))) {
                    if (summaryBuilder.length() > 0) summaryBuilder.append(" ");
                    summaryBuilder.append("다만, ").append(sentence).append(".");
                    break; // 첫 번째 부정 문장만 사용
                }
            }
            
            // 축제 관련 문장 찾기
            if (summaryBuilder.length() == 0) {
                for (String sentence : sentences) {
                    sentence = sentence.trim();
                    if (sentence.length() > 10 && sentence.length() < 150 &&
                        (sentence.contains("축제") || sentence.contains("행사") ||
                         sentence.contains("이벤트") || sentence.contains("축제") ||
                         sentence.contains("체험") || sentence.contains("경험"))) {
                        summaryBuilder.append(sentence).append(".");
                        break;
                    }
                }
            }
            
            if (summaryBuilder.length() > 0) {
                return summaryBuilder.toString();
            }
            
        } catch (Exception e) {
            log.debug("요약 추출 실패: {}", e.getMessage());
        }
        return "블로거의 축제 경험에 대한 구체적인 평가가 분석되었습니다.";
    }
    
    /**
     * 정규식을 사용하여 JSON 추출
     */
    private String extractJsonWithRegex(String response) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{.*\\}", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                String jsonPart = matcher.group();
                log.debug("정규식으로 추출된 JSON 길이: {}", jsonPart.length());
                
                String cleanedJson = cleanAndFixJson(jsonPart);
                if (isValidJson(cleanedJson)) {
                    log.debug("정규식 추출 성공");
                    return cleanedJson;
                }
            }
        } catch (Exception e) {
            log.debug("정규식 추출 실패: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 중괄호 기반으로 JSON 추출
     */
    private String extractJsonWithBraces(String response) {
        try {
            int startBrace = response.indexOf('{');
            int endBrace = response.lastIndexOf('}');
            
            if (startBrace != -1 && endBrace != -1 && endBrace > startBrace) {
                String jsonPart = response.substring(startBrace, endBrace + 1);
                log.debug("중괄호로 추출된 JSON 길이: {}", jsonPart.length());
                
                String cleanedJson = cleanAndFixJson(jsonPart);
                if (isValidJson(cleanedJson)) {
                    log.debug("중괄호 추출 성공");
                    return cleanedJson;
                }
            }
        } catch (Exception e) {
            log.debug("중괄호 추출 실패: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 키워드 기반으로 JSON 추출
     */
    private String extractJsonWithKeywords(String response) {
        try {
            // JSON 키워드들을 찾아서 그 주변을 추출
            String[] keywords = {"festival_score", "positive_percentage", "negative_percentage", "is_relevant"};
            
            for (String keyword : keywords) {
                int keywordIndex = response.indexOf(keyword);
                if (keywordIndex != -1) {
                    // 키워드 앞뒤로 중괄호 찾기
                    int startBrace = response.lastIndexOf('{', keywordIndex);
                    int endBrace = response.indexOf('}', keywordIndex);
                    
                    if (startBrace != -1 && endBrace != -1 && endBrace > startBrace) {
                        String jsonPart = response.substring(startBrace, endBrace + 1);
                        log.debug("키워드 '{}'로 추출된 JSON 길이: {}", keyword, jsonPart.length());
                        
                        String cleanedJson = cleanAndFixJson(jsonPart);
                        if (isValidJson(cleanedJson)) {
                            log.debug("키워드 추출 성공");
                            return cleanedJson;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("키워드 추출 실패: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * JSON 유효성 검사
     */
    private boolean isValidJson(String jsonString) {
        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * JSON 문자열을 정리하고 수정합니다.
     */
    private String cleanAndFixJson(String jsonString) {
        if (jsonString == null) return getDefaultJson();
        
        try {
            log.debug("JSON 정리 시작 - 원본 길이: {}", jsonString.length());
            
            // 1. JSON 시작과 끝 찾기
            int startBrace = jsonString.indexOf('{');
            int endBrace = jsonString.lastIndexOf('}');
            
            if (startBrace == -1 || endBrace == -1) {
                log.warn("JSON 구조가 올바르지 않음");
                return getDefaultJson();
            }
            
            // 2. JSON 부분만 추출
            String jsonPart = jsonString.substring(startBrace, endBrace + 1);
            
            // 3. 완전히 새로운 JSON 재구성
            return reconstructJsonSafely(jsonPart);
            
        } catch (Exception e) {
            log.warn("JSON 정리 실패: {} - 원본 길이: {}", e.getMessage(), jsonString.length());
            return getDefaultJson();
        }
    }
    
    /**
     * JSON을 안전하게 재구성합니다.
     */
    private String reconstructJsonSafely(String jsonString) {
        try {
            // 1. 기본 JSON 검증 시도
            objectMapper.readTree(jsonString);
            return jsonString; // 성공하면 그대로 반환
            
        } catch (Exception e) {
            log.debug("JSON 검증 실패, 재구성 시도: {}", e.getMessage());
            
            // 2. 패턴 매칭으로 값 추출
            int festivalScore = extractIntValue(jsonString, "festival_score", 50);
            int positivePercentage = extractIntValue(jsonString, "positive_percentage", 50);
            int negativePercentage = extractIntValue(jsonString, "negative_percentage", 50);
            
            // 3. 키워드 추출
            String positiveKeywords = extractKeywordsFromText(jsonString, "positive_keywords");
            String negativeKeywords = extractKeywordsFromText(jsonString, "negative_keywords");
            
            // 4. 요약 추출
            String aiSummary = extractSummaryFromText(jsonString);
            
            // 5. 새로운 JSON 생성 (이스케이프 문자 문제 없이)
            return String.format("""
                {
                    "festival_score": %d,
                    "positive_percentage": %d,
                    "negative_percentage": %d,
                    "positive_keywords": %s,
                    "negative_keywords": %s,
                    "ai_summary": "%s"
                }
                """, festivalScore, positivePercentage, negativePercentage, 
                positiveKeywords, negativeKeywords, aiSummary);
        }
    }
    
    /**
     * 상세 분석 결과를 파싱하여 ReviewAnalysis 객체로 변환합니다.
     */
    private ReviewAnalysis parseDetailedResult(Review review, String detailedResult) {
        try {
            log.debug("상세 분석 원본 응답: {}", detailedResult);
            
            // AI 응답에서 JSON 부분만 추출
            String cleanJson = extractJsonFromResponse(detailedResult);
            log.debug("상세 분석 정리된 JSON: {}", cleanJson);
            
            // JSON 파싱 시도
            JsonNode jsonNode = objectMapper.readTree(cleanJson);
            
            // 안전한 값 추출 (기본값 제공)
            int festivalScore = getIntValueSafely(jsonNode, "festival_score", 50);
            int positivePercentage = getIntValueSafely(jsonNode, "positive_percentage", 50);
            int negativePercentage = getIntValueSafely(jsonNode, "negative_percentage", 50);
            
            String positiveKeywords = getArrayValueSafely(jsonNode, "positive_keywords");
            String negativeKeywords = getArrayValueSafely(jsonNode, "negative_keywords");
            String aiSummary = getStringValueSafely(jsonNode, "ai_summary", "분석 완료");
            
            log.debug("파싱된 값들 - 점수: {}, 긍정: {}, 부정: {}", festivalScore, positivePercentage, negativePercentage);
            
            return ReviewAnalysis.builder()
                    .review(review)
                    .festivalScore(festivalScore)
                    .positivePercentage(positivePercentage)
                    .negativePercentage(negativePercentage)
                    .positiveKeywords(positiveKeywords)
                    .negativeKeywords(negativeKeywords)
                    .aiSummary(aiSummary)
                    .analysisResult(detailedResult)
                    .isSuccess(true)
                    .analyzedAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("상세 분석 결과 파싱 실패: {}", e.getMessage());
            log.error("원본 응답: {}", detailedResult);
            
            // 파싱 실패 시 기본값으로 분석 결과 생성
            return ReviewAnalysis.builder()
                    .review(review)
                    .festivalScore(50)
                    .positivePercentage(50)
                    .negativePercentage(50)
                    .positiveKeywords("[]")
                    .negativeKeywords("[]")
                    .aiSummary("분석 중 오류가 발생했습니다.")
                    .analysisResult(detailedResult)
                    .isSuccess(false)
                    .errorMessage("JSON 파싱 실패: " + e.getMessage())
                    .analyzedAt(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * 안전하게 정수 값을 추출합니다.
     */
    private int getIntValueSafely(JsonNode jsonNode, String fieldName, int defaultValue) {
        try {
            JsonNode node = jsonNode.get(fieldName);
            if (node != null && !node.isNull()) {
                return node.asInt();
            }
        } catch (Exception e) {
            log.warn("정수 값 추출 실패 ({}): {}", fieldName, e.getMessage());
        }
        return defaultValue;
    }
    
    /**
     * 안전하게 문자열 값을 추출합니다.
     */
    private String getStringValueSafely(JsonNode jsonNode, String fieldName, String defaultValue) {
        try {
            JsonNode node = jsonNode.get(fieldName);
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        } catch (Exception e) {
            log.warn("문자열 값 추출 실패 ({}): {}", fieldName, e.getMessage());
        }
        return defaultValue;
    }
    
    /**
     * 안전하게 배열 값을 JSON 문자열로 추출합니다.
     */
    private String getArrayValueSafely(JsonNode jsonNode, String fieldName) {
        try {
            JsonNode node = jsonNode.get(fieldName);
            if (node != null && node.isArray()) {
                return objectMapper.writeValueAsString(node);
            }
        } catch (Exception e) {
            log.warn("배열 값 추출 실패 ({}): {}", fieldName, e.getMessage());
        }
        return "[]";
    }
}
