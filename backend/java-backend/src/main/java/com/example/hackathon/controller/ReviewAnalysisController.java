package com.example.hackathon.controller;

import com.example.hackathon.entity.Review;
import com.example.hackathon.entity.ReviewAnalysis;
import com.example.hackathon.repository.ReviewRepository;
import com.example.hackathon.repository.ReviewAnalysisRepository;
import com.example.hackathon.service.ReviewAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 리뷰 분석 API를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class ReviewAnalysisController {
    
    private final ReviewAnalysisService reviewAnalysisService;
    private final ReviewRepository reviewRepository;
    private final ReviewAnalysisRepository reviewAnalysisRepository;
    
    /**
     * 특정 리뷰를 분석합니다.
     */
    @PostMapping("/review/{reviewId}")
    public ResponseEntity<ReviewAnalysis> analyzeReview(@PathVariable Long reviewId) {
        log.info("리뷰 분석 요청: {}", reviewId);
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));
        
        ReviewAnalysis analysis = reviewAnalysisService.analyzeReview(review);
        return ResponseEntity.ok(analysis);
    }
    
    /**
     * 축제의 모든 리뷰를 분석합니다.
     */
    @PostMapping("/festival/{festivalName}")
    public ResponseEntity<List<ReviewAnalysis>> analyzeFestival(@PathVariable String festivalName) {
        log.info("축제 리뷰 분석 요청: {}", festivalName);
        
        try {
            List<ReviewAnalysis> analyses = reviewAnalysisService.analyzeFestivalReviews(festivalName);
            return ResponseEntity.ok(analyses);
        } catch (InterruptedException e) {
            log.error("축제 리뷰 분석 중 인터럽트 발생: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
    /**
     * 특정 분석 데이터를 삭제합니다.
     */
    @DeleteMapping("/{analysisId}")
    public ResponseEntity<Map<String, Object>> deleteAnalysis(@PathVariable Long analysisId) {
        log.info("분석 데이터 삭제 요청: {}", analysisId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ReviewAnalysis analysis = reviewAnalysisRepository.findById(analysisId)
                    .orElse(null);
            
            if (analysis == null) {
                response.put("success", false);
                response.put("message", "해당 분석 데이터를 찾을 수 없습니다: " + analysisId);
                return ResponseEntity.ok(response);
            }
            
            reviewAnalysisRepository.delete(analysis);
            
            response.put("success", true);
            response.put("message", "분석 데이터가 성공적으로 삭제되었습니다.");
            response.put("deletedAnalysisId", analysisId);
            
            log.info("분석 데이터 삭제 완료: ID={}", analysisId);
            
        } catch (Exception e) {
            log.error("분석 데이터 삭제 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "분석 데이터 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 여러 분석 데이터를 한 번에 삭제합니다.
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteMultipleAnalyses(@RequestBody List<Long> analysisIds) {
        log.info("다중 분석 데이터 삭제 요청: {}개", analysisIds.size());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            int deletedCount = 0;
            List<Long> notFoundIds = new ArrayList<>();
            
            for (Long analysisId : analysisIds) {
                try {
                    ReviewAnalysis analysis = reviewAnalysisRepository.findById(analysisId).orElse(null);
                    
                    if (analysis != null) {
                        reviewAnalysisRepository.delete(analysis);
                        deletedCount++;
                        log.info("분석 데이터 삭제 완료: ID={}", analysisId);
                    } else {
                        notFoundIds.add(analysisId);
                    }
                } catch (Exception e) {
                    log.error("분석 데이터 {} 삭제 실패: {}", analysisId, e.getMessage());
                    notFoundIds.add(analysisId);
                }
            }
            
            response.put("success", true);
            response.put("message", String.format("삭제 완료: %d개 분석 데이터", deletedCount));
            response.put("deletedCount", deletedCount);
            response.put("notFoundIds", notFoundIds);
            
        } catch (Exception e) {
            log.error("다중 분석 데이터 삭제 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "다중 분석 데이터 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 모든 분석 데이터를 삭제합니다.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAllAnalysisData() {
        log.info("모든 분석 데이터 삭제 요청");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            long deletedCount = reviewAnalysisRepository.count();
            reviewAnalysisRepository.deleteAll();
            
            response.put("success", true);
            response.put("message", "모든 분석 데이터가 삭제되었습니다.");
            response.put("deletedCount", deletedCount);
            
            log.info("분석 데이터 삭제 완료: {}개", deletedCount);
            
        } catch (Exception e) {
            log.error("분석 데이터 삭제 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "분석 데이터 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 축제의 분석 통계를 조회합니다.
     */
    @GetMapping("/festival/{festivalName}/stats")
    public ResponseEntity<Map<String, Object>> getFestivalStats(@PathVariable String festivalName) {
        log.info("축제 분석 통계 조회: {}", festivalName);
        
        // 해당 축제의 총 리뷰 수
        long totalReviews = reviewRepository.countByFestivalName(festivalName);
        
        // 분석된 리뷰 수
        List<ReviewAnalysis> analyzedReviews = reviewAnalysisRepository.findByFestivalNameAndSuccess(festivalName);
        long analyzedCount = analyzedReviews.size();
        
        // 평균 축제 점수
        Double averageScore = reviewAnalysisRepository.getAverageFestivalScoreByFestival(festivalName);
        double avgScore = averageScore != null ? averageScore : 0.0;
        
        // 점수 분포 (관련 있는 리뷰만)
        Map<String, Long> scoreDistribution = Map.of(
            "매우나쁨(0-20)", analyzedReviews.stream().filter(a -> a.getFestivalScore() != null && a.getFestivalScore() <= 20).count(),
            "나쁨(21-40)", analyzedReviews.stream().filter(a -> a.getFestivalScore() != null && a.getFestivalScore() >= 21 && a.getFestivalScore() <= 40).count(),
            "보통(41-60)", analyzedReviews.stream().filter(a -> a.getFestivalScore() != null && a.getFestivalScore() >= 41 && a.getFestivalScore() <= 60).count(),
            "좋음(61-80)", analyzedReviews.stream().filter(a -> a.getFestivalScore() != null && a.getFestivalScore() >= 61 && a.getFestivalScore() <= 80).count(),
            "매우좋음(81-100)", analyzedReviews.stream().filter(a -> a.getFestivalScore() != null && a.getFestivalScore() >= 81).count(),
            "관련없음", analyzedReviews.stream().filter(a -> a.getFestivalScore() == null).count()
        );
        
        Map<String, Object> stats = Map.of(
            "festivalName", festivalName,
            "totalReviews", totalReviews,
            "analyzedReviews", analyzedCount,
            "averageFestivalScore", Math.round(avgScore * 10.0) / 10.0, // 소수점 1자리
            "scoreDistribution", scoreDistribution
        );
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 분석 결과를 조회합니다.
     */
    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> getAnalysisResults() {
        
        log.info("분석 결과 조회");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 모든 분석 결과 조회 (성공한 것만)
            List<ReviewAnalysis> allAnalyses = reviewAnalysisRepository.findByIsSuccessTrueOrderByCreatedAtDesc();
            
            response.put("success", true);
            response.put("totalCount", allAnalyses.size());
            response.put("results", allAnalyses);
            
            log.info("분석 결과 조회 완료: 총 {}개 반환", allAnalyses.size());
            
        } catch (Exception e) {
            log.error("분석 결과 조회 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "분석 결과 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
