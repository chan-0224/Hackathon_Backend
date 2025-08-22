package com.example.hackathon.repository;

import com.example.hackathon.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 축제 리뷰 데이터 접근을 위한 Repository
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    /**
     * 특정 축제 이름으로 리뷰 검색
     */
    List<Review> findByFestivalNameContainingOrderByCreatedAtDesc(String festivalName);
    
    /**
     * 정확한 축제 이름으로 리뷰 검색
     */
    List<Review> findByFestivalName(String festivalName);
    
    /**
     * 특정 축제의 성공한 리뷰만 검색 (실제 수집된 개수)
     */
    List<Review> findByFestivalNameAndIsSuccessTrue(String festivalName);
    
    /**
     * 특정 축제의 총 리뷰 개수 조회
     */
    long countByFestivalName(String festivalName);
    
    /**
     * 크롤링 성공한 리뷰만 조회
     */
    List<Review> findByIsSuccessTrueOrderByCreatedAtDesc();
    
    /**
     * 특정 기간 내에 크롤링된 리뷰 조회
     */
    List<Review> findByCrawledAtBetweenOrderByCrawledAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 특정 축제의 성공한 리뷰 개수 조회
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.festivalName = :festivalName AND r.isSuccess = true")
    long countSuccessfulReviewsByFestivalName(@Param("festivalName") String festivalName);
    
    /**
     * 중복 URL 체크 (같은 블로그 글을 다시 크롤링하지 않도록)
     */
    boolean existsByBlogUrl(String blogUrl);
    
    /**
     * 최근 크롤링된 리뷰들 조회 (모니터링용)
     */
    @Query("SELECT r FROM Review r ORDER BY r.crawledAt DESC")
    List<Review> findRecentReviews(@Param("limit") int limit);
    
    /**
     * 실패한 리뷰들 조회
     */
    List<Review> findByIsSuccessFalseOrderByCreatedAtDesc();
}
