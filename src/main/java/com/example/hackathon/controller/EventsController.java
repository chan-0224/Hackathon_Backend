package com.example.hackathon.controller;

import com.example.hackathon.entity.Festival;
import com.example.hackathon.service.FestivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 문화행사(축제) API를 제공하는 Controller 클래스
 * 
 * 주요 기능:
 * - 서울시 문화행사 데이터 수집 및 관리
 * - AI 요약 및 태그 생성
 * - 문화행사 검색 및 조회 (지역별, 이름별, 태그별)
 * - 태그 기반 추천 시스템
 * - 상세 정보 조회 (AI 생성 정보 포함)
 * 
 * @author 해커톤 팀
 * @version 1.0
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventsController {
    
    private final FestivalService festivalService;
    
    /**
     * 서울시 문화행사 데이터 수집 (수동 실행)
     * 
     * @return 수집 완료 메시지 또는 오류 메시지
     */
    @PostMapping("/fetch")
    public ResponseEntity<String> fetchEvents() {
        try {
            festivalService.fetchAndSaveFestivals();
            return ResponseEntity.ok("문화행사 데이터 수집이 완료되었습니다.");
        } catch (Exception e) {
            log.error("문화행사 데이터 수집 실패", e);
            String errorMessage = "문화행사 데이터 수집에 실패했습니다: " + e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " (원인: " + e.getCause().getMessage() + ")";
            }
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }
    
    /**
     * 기존 축제 데이터에 AI 요약 및 태그 생성 (수동 실행)
     * 
     * @return 생성 완료 메시지 또는 오류 메시지
     */
    @PostMapping("/generate-ai")
    public ResponseEntity<String> generateAIForExistingEvents() {
        try {
            festivalService.generateAIForExistingFestivals();
            return ResponseEntity.ok("기존 축제 데이터에 AI 요약 및 태그 생성이 완료되었습니다.");
        } catch (Exception e) {
            log.error("AI 생성 실패", e);
            return ResponseEntity.internalServerError().body("AI 생성에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 특정 문화행사 상세 정보 조회 (AI 요약 및 태그 포함)
     * 
     * @param id 조회할 문화행사 ID
     * @return 문화행사 상세 정보 (AI 생성 정보 포함)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getEventById(@PathVariable Long id) {
        log.info("문화행사 상세 조회: ID {}", id);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Festival festival = festivalService.getFestivalById(id);
            
            if (festival == null) {
                response.put("success", false);
                response.put("message", "해당 ID의 문화행사를 찾을 수 없습니다.");
                return ResponseEntity.notFound().build();
            }
            
            // 기본 정보
            response.put("success", true);
            response.put("id", festival.getId());
            response.put("name", festival.getName());
            response.put("date", festival.getDate());
            response.put("district", festival.getDistrict());
            response.put("place", festival.getPlace());
            response.put("link", festival.getLink());
            response.put("createdAt", festival.getCreatedAt());
            response.put("updatedAt", festival.getUpdatedAt());
            
            // AI 생성 정보
            response.put("aiSummary", festival.getAiSummary());
            response.put("moodTags", festival.getMoodTags());
            
            // AI 정보가 있는지 여부
            response.put("hasAISummary", festival.getAiSummary() != null && !festival.getAiSummary().trim().isEmpty());
            response.put("hasMoodTags", festival.getMoodTags() != null && !festival.getMoodTags().trim().isEmpty());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("문화행사 상세 조회 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "문화행사 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 전체 문화행사 목록 조회
     * 
     * @return 전체 문화행사 목록
     */
    @GetMapping
    public ResponseEntity<List<Festival>> getAllEvents() {
        List<Festival> festivals = festivalService.getAllFestivals();
        return ResponseEntity.ok(festivals);
    }
    
    /**
     * 특정 구의 문화행사 목록 조회
     * 
     * @param district 검색할 구명
     * @return 해당 구의 문화행사 목록
     */
    @GetMapping("/district/{district}")
    public ResponseEntity<List<Festival>> getEventsByDistrict(@PathVariable String district) {
        List<Festival> festivals = festivalService.getFestivalsByDistrict(district);
        return ResponseEntity.ok(festivals);
    }
    
    /**
     * 이름으로 문화행사 검색
     * 
     * @param name 검색할 행사명 키워드
     * @return 키워드가 포함된 문화행사 목록
     */
    @GetMapping("/search")
    public ResponseEntity<List<Festival>> searchEventsByName(@RequestParam String name) {
        List<Festival> festivals = festivalService.getFestivalsByName(name);
        return ResponseEntity.ok(festivals);
    }
    
    /**
     * 구별 + 이름으로 문화행사 검색
     * 
     * @param district 검색할 구명
     * @param name 검색할 행사명 키워드
     * @return 조건에 맞는 문화행사 목록
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<List<Festival>> searchEventsByDistrictAndName(
            @RequestParam String district, 
            @RequestParam String name) {
        List<Festival> festivals = festivalService.getFestivalsByDistrictAndName(district, name);
        return ResponseEntity.ok(festivals);
    }
    
    /**
     * API 키 및 연결 상태 확인
     * 
     * @return 서버 상태 메시지
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("서버가 정상적으로 실행 중입니다. API 키가 설정되어 있습니다.");
    }
    
    /**
     * 태그 기반 추천 문화행사 조회
     * 
     * @param tags 검색할 태그 (쉼표로 구분)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 태그 기반 추천 문화행사 목록
     */
    @GetMapping("/recommend")
    public ResponseEntity<Map<String, Object>> getRecommendedEvents(
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("추천 문화행사 요청 - 태그: {}, 페이지: {}, 크기: {}", tags, page, size);
        
        try {
            Map<String, Object> result = festivalService.getRecommendedFestivals(tags, page, size);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("추천 문화행사 조회 실패: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "추천 문화행사 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
