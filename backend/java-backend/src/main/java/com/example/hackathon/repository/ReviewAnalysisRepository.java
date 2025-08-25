package com.example.hackathon.repository;

import com.example.hackathon.entity.ReviewAnalysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 리뷰 분석 결과를 관리하는 Repository
 */
@Repository
public interface ReviewAnalysisRepository extends JpaRepository<ReviewAnalysis, Long> {
    
    /**
     * 특정 리뷰의 분석 결과를 조회합니다.
     */
    List<ReviewAnalysis> findByReviewIdOrderByCreatedAtDesc(Long reviewId);
    
    /**
     * 성공한 분석 결과만 조회합니다.
     */
    List<ReviewAnalysis> findByIsSuccessTrueOrderByCreatedAtDesc();
    

    
    /**
     * 실패한 분석 결과만 조회합니다.
     */
    List<ReviewAnalysis> findByIsSuccessFalseOrderByCreatedAtDesc();
    
    /**
     * 특정 축제의 분석 결과를 조회합니다.
     */
    @Query("SELECT ra FROM ReviewAnalysis ra " +
           "JOIN ra.review r " +
           "WHERE r.festivalName = :festivalName " +
           "AND ra.isSuccess = true " +
           "ORDER BY ra.createdAt DESC")
    List<ReviewAnalysis> findByFestivalNameAndSuccess(@Param("festivalName") String festivalName);
    
    /**
     * 특정 축제의 평균 축제 점수를 조회합니다.
     */
    @Query("SELECT AVG(ra.festivalScore) FROM ReviewAnalysis ra " +
           "JOIN ra.review r " +
           "WHERE r.festivalName = :festivalName " +
           "AND ra.isSuccess = true")
    Double getAverageFestivalScoreByFestival(@Param("festivalName") String festivalName);
}
