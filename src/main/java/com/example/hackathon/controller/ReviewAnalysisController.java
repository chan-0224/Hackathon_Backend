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
        
        List<ReviewAnalysis> analyses = reviewAnalysisService.analyzeFestivalReviews(festivalName);
        return ResponseEntity.ok(analyses);
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
    public ResponseEntity<List<ReviewAnalysis>> getAnalysisResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("분석 결과 조회: page={}, size={}", page, size);
        
        // TODO: 페이징 처리된 분석 결과 조회
        return ResponseEntity.ok(List.of());
    }
}
