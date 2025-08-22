package com.example.hackathon.repository;

import com.example.hackathon.entity.Festival;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 문화행사(축제) 데이터 접근을 위한 Repository 인터페이스
 * 
 * 주요 기능:
 * - 기본 CRUD 작업 (JpaRepository 상속)
 * - 지역별, 이름별 행사 검색
 * - 태그 기반 추천 시스템을 위한 쿼리
 * - 챗봇을 위한 키워드 검색 기능
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long> {
    
    /**
     * 특정 지역의 행사 목록 조회
     * 
     * @param district 검색할 지역 (구 단위)
     * @return 해당 지역의 행사 목록
     */
    List<Festival> findByDistrict(String district);
    
    /**
     * 행사명에 특정 키워드가 포함된 행사 목록 조회
     * 
     * @param name 검색할 키워드
     * @return 키워드가 포함된 행사 목록
     */
    List<Festival> findByNameContaining(String name);
    
    /**
     * 특정 지역에서 행사명에 키워드가 포함된 행사 목록 조회
     * 
     * @param district 검색할 지역 (구 단위)
     * @param name 검색할 키워드
     * @return 조건에 맞는 행사 목록
     */
    List<Festival> findByDistrictAndNameContaining(String district, String name);
    
    /**
     * 고유 키로 행사 존재 여부 확인
     * 
     * @param uniqueKey 확인할 고유 키
     * @return 존재 여부
     */
    boolean existsByUniqueKey(String uniqueKey);
    
    /**
     * 챗봇용 키워드 검색
     * mood_tags 필드에서 특정 키워드를 포함하는 행사 검색
     * 
     * @param keyword 검색할 키워드
     * @return 키워드가 포함된 행사 목록
     */
    @Query(value = """
        SELECT * FROM festivals f 
        WHERE f.mood_tags IS NOT NULL 
        AND f.mood_tags LIKE CONCAT('%', :keyword, '%')
        ORDER BY f.created_at DESC
        """, nativeQuery = true)
    List<Festival> findByMoodTagsContaining(@Param("keyword") String keyword);
    
    /**
     * 태그 기반 추천 쿼리 (페이징 지원)
     * mood_tags 필드에서 특정 태그를 포함하는 행사 검색
     * 
     * @param tag 검색할 태그
     * @param pageable 페이징 정보
     * @return 태그가 포함된 행사 페이지
     */
    @Query(value = """
        SELECT * FROM festivals f 
        WHERE f.mood_tags IS NOT NULL 
        AND f.mood_tags LIKE CONCAT('%', :tag, '%')
        ORDER BY f.created_at DESC
        """, nativeQuery = true)
    Page<Festival> findFestivalsByTagWithPaging(@Param("tag") String tag, Pageable pageable);
    
    /**
     * 태그 기반 추천 쿼리 (전체 개수)
     * mood_tags 필드에서 특정 태그를 포함하는 행사의 총 개수 조회
     * 
     * @param tag 검색할 태그
     * @return 태그가 포함된 행사의 총 개수
     */
    @Query(value = """
        SELECT COUNT(*) 
        FROM festivals f 
        WHERE f.mood_tags IS NOT NULL 
        AND f.mood_tags LIKE CONCAT('%', :tag, '%')
        """, nativeQuery = true)
    long countFestivalsByTag(@Param("tag") String tag);
}
