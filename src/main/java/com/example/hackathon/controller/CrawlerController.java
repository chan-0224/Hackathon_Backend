package com.example.hackathon.controller;

import com.example.hackathon.entity.Review;
import com.example.hackathon.entity.ReviewAnalysis;
import com.example.hackathon.repository.ReviewRepository;
import com.example.hackathon.repository.ReviewAnalysisRepository;
import com.example.hackathon.service.NaverBlogCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 크롤링 관련 API를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
@Slf4j
public class CrawlerController {
    
    private final NaverBlogCrawlerService crawlerService;
    private final ReviewRepository reviewRepository;
    private final ReviewAnalysisRepository reviewAnalysisRepository;
    
    /**
     * 축제 리뷰 크롤링을 시작합니다.
     * 
     * @param request 크롤링 요청 정보
     * @return 크롤링 결과
     */
    @PostMapping("/reviews")
    public ResponseEntity<Map<String, Object>> crawlFestivalReviews(@RequestBody CrawlRequest request) {
        log.info("축제 리뷰 크롤링 요청: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 크롤링 실행
            List<Review> reviews = crawlerService.crawlFestivalReviews(
                request.getFestivalName(), 
                request.getMaxReviews()
            );
            
            response.put("success", true);
            response.put("message", "크롤링이 완료되었습니다.");
            response.put("crawledCount", reviews.size());
            response.put("reviews", reviews);
            
            log.info("크롤링 완료: {}개 리뷰 수집", reviews.size());
            
        } catch (Exception e) {
            log.error("크롤링 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "크롤링 중 오류가 발생했습니다: " + e.getMessage());
            response.put("crawledCount", 0);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 축제의 크롤링된 리뷰 목록을 조회합니다.
     */
    @GetMapping("/reviews/{festivalName}")
    public ResponseEntity<Map<String, Object>> getFestivalReviews(@PathVariable String festivalName) {
        log.info("축제 리뷰 조회: {}", festivalName);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Review> reviews = reviewRepository.findByFestivalNameContainingOrderByCreatedAtDesc(festivalName);
            
            response.put("success", true);
            response.put("festivalName", festivalName);
            response.put("totalCount", reviews.size());
            response.put("reviews", reviews);
            
        } catch (Exception e) {
            log.error("리뷰 조회 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "리뷰 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 크롤링된 모든 리뷰 목록을 조회합니다.
     */
    @GetMapping("/reviews")
    public ResponseEntity<Map<String, Object>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("전체 리뷰 조회: page={}, size={}", page, size);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Review> reviews = reviewRepository.findByIsSuccessTrueOrderByCreatedAtDesc();
            
            // 페이징 처리
            int start = page * size;
            int end = Math.min(start + size, reviews.size());
            
            List<Review> pagedReviews = reviews.subList(start, end);
            
            response.put("success", true);
            response.put("totalCount", reviews.size());
            response.put("page", page);
            response.put("size", size);
            response.put("reviews", pagedReviews);
            
        } catch (Exception e) {
            log.error("전체 리뷰 조회 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "리뷰 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 크롤링 통계 정보를 조회합니다.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCrawlingStats() {
        log.info("크롤링 및 분석 통계 조회");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 크롤링 통계
            long totalReviews = reviewRepository.count();
            long successfulReviews = reviewRepository.findByIsSuccessTrueOrderByCreatedAtDesc().size();
            long failedReviews = totalReviews - successfulReviews;
            
            // 분석 통계
            long totalAnalyses = reviewAnalysisRepository.count();
            long successfulAnalyses = reviewAnalysisRepository.findByIsSuccessTrueOrderByCreatedAtDesc().size();
            long failedAnalyses = totalAnalyses - successfulAnalyses;
            
            // 최근 크롤링된 리뷰들 조회 (최근 20개)
            List<Review> recentReviews = reviewRepository.findRecentReviews(20);
            
            // 축제별 통계
            Map<String, Long> festivalStats = new HashMap<>();
            for (Review review : recentReviews) {
                String festivalName = review.getFestivalName();
                festivalStats.put(festivalName, festivalStats.getOrDefault(festivalName, 0L) + 1);
            }
            
            // 실패한 리뷰들의 오류 메시지 분석
            List<Review> failedReviewsList = reviewRepository.findByIsSuccessFalseOrderByCreatedAtDesc();
            Map<String, Long> errorStats = new HashMap<>();
            for (Review review : failedReviewsList) {
                String errorMsg = review.getErrorMessage();
                if (errorMsg != null) {
                    String errorType = errorMsg.contains("차단") ? "차단됨" : 
                                     errorMsg.contains("timeout") ? "타임아웃" : 
                                     errorMsg.contains("404") ? "페이지 없음" : "기타 오류";
                    errorStats.put(errorType, errorStats.getOrDefault(errorType, 0L) + 1);
                }
            }
            
            // 분석 실패 통계
            List<ReviewAnalysis> failedAnalysesList = reviewAnalysisRepository.findByIsSuccessTrueOrderByCreatedAtDesc();
            Map<String, Long> analysisErrorStats = new HashMap<>();
            for (ReviewAnalysis analysis : failedAnalysesList) {
                String errorMsg = analysis.getErrorMessage();
                if (errorMsg != null) {
                    String errorType = errorMsg.contains("API") ? "API 오류" : 
                                     errorMsg.contains("토큰") ? "토큰 오류" : 
                                     errorMsg.contains("timeout") ? "타임아웃" : "기타 오류";
                    analysisErrorStats.put(errorType, analysisErrorStats.getOrDefault(errorType, 0L) + 1);
                }
            }
            
            // 최근 분석 결과 (최근 10개)
            List<ReviewAnalysis> recentAnalyses = reviewAnalysisRepository.findByIsSuccessTrueOrderByCreatedAtDesc().stream()
                    .limit(10)
                    .collect(Collectors.toList());
            
            response.put("success", true);
            
            // 크롤링 통계
            response.put("totalReviews", totalReviews);
            response.put("successfulReviews", successfulReviews);
            response.put("failedReviews", failedReviews);
            response.put("successRate", totalReviews > 0 ? Math.round((double) successfulReviews / totalReviews * 10000) / 100.0 : 0.0);
            
            // 분석 통계
            response.put("totalAnalyses", totalAnalyses);
            response.put("successfulAnalyses", successfulAnalyses);
            response.put("failedAnalyses", failedAnalyses);
            response.put("analysisSuccessRate", totalAnalyses > 0 ? Math.round((double) successfulAnalyses / totalAnalyses * 10000) / 100.0 : 0.0);
            
            response.put("festivalStats", festivalStats);
            response.put("errorStats", errorStats);
            response.put("analysisErrorStats", analysisErrorStats);
            response.put("recentReviews", recentReviews.stream().map(r -> {
                Map<String, Object> reviewMap = new HashMap<>();
                reviewMap.put("id", r.getId());
                reviewMap.put("festivalName", r.getFestivalName() != null ? r.getFestivalName() : "");
                reviewMap.put("title", r.getTitle() != null ? r.getTitle() : "");
                reviewMap.put("isSuccess", r.getIsSuccess());
                reviewMap.put("errorMessage", r.getErrorMessage());
                reviewMap.put("crawledAt", r.getCrawledAt());
                return reviewMap;
            }).collect(Collectors.toList()));
            
            response.put("recentAnalyses", recentAnalyses.stream().map(a -> {
                Map<String, Object> analysisMap = new HashMap<>();
                analysisMap.put("id", a.getId());
                analysisMap.put("reviewId", a.getReview().getId());
                analysisMap.put("festivalName", a.getReview().getFestivalName() != null ? a.getReview().getFestivalName() : "");
                analysisMap.put("festivalScore", a.getFestivalScore());
                analysisMap.put("isSuccess", a.getIsSuccess());
                analysisMap.put("errorMessage", a.getErrorMessage());
                analysisMap.put("analyzedAt", a.getAnalyzedAt());
                return analysisMap;
            }).collect(Collectors.toList()));
            
        } catch (Exception e) {
            log.error("통계 조회 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "통계 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 모든 데이터를 초기화합니다. (개발용)
     */
    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetAllData() {
        log.info("모든 데이터 초기화");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 분석 데이터 삭제
            reviewAnalysisRepository.deleteAll();
            
            // 리뷰 데이터 삭제
            reviewRepository.deleteAll();
            
            response.put("success", true);
            response.put("message", "모든 데이터가 초기화되었습니다.");
            
        } catch (Exception e) {
            log.error("데이터 초기화 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "데이터 초기화 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 크롤링 요청 DTO
     */
    public static class CrawlRequest {
        private String festivalName;
        private int maxReviews = 20;
        
        // Getters and Setters
        public String getFestivalName() {
            return festivalName;
        }
        
        public void setFestivalName(String festivalName) {
            this.festivalName = festivalName;
        }
        
        public int getMaxReviews() {
            return maxReviews;
        }
        
        public void setMaxReviews(int maxReviews) {
            this.maxReviews = maxReviews;
        }
        
        @Override
        public String toString() {
            return "CrawlRequest{" +
                    "festivalName='" + festivalName + '\'' +
                    ", maxReviews=" + maxReviews +
                    '}';
        }
    }
}
