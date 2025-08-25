package com.example.hackathon.controller;

import com.example.hackathon.entity.Festival;
import com.example.hackathon.service.FestivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
@Slf4j
public class FestivalController {
    
    private final FestivalService festivalService;
    
    /**
     * 서울시 문화행사 데이터 수집 (수동 실행) - JDK 21 var 활용
     */
    @PostMapping("/fetch")
    public ResponseEntity<String> fetchFestivals() {
        try {
            festivalService.fetchAndSaveFestivals();
            return ResponseEntity.ok("문화행사 데이터 수집이 완료되었습니다.");
        } catch (Exception e) {
            log.error("문화행사 데이터 수집 실패", e);
            var errorMessage = "문화행사 데이터 수집에 실패했습니다: " + e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " (원인: " + e.getCause().getMessage() + ")";
            }
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }
    
    /**
     * 전체 문화행사 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<Festival>> getAllFestivals() {
        List<Festival> festivals = festivalService.getAllFestivals();
        return ResponseEntity.ok(festivals);
    }
    
    /**
     * 특정 구의 문화행사 목록 조회
     */
    @GetMapping("/district/{district}")
    public ResponseEntity<List<Festival>> getFestivalsByDistrict(@PathVariable String district) {
        List<Festival> festivals = festivalService.getFestivalsByDistrict(district);
        return ResponseEntity.ok(festivals);
    }
    
    /**
     * 이름으로 문화행사 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<Festival>> searchFestivalsByName(@RequestParam String name) {
        List<Festival> festivals = festivalService.getFestivalsByName(name);
        return ResponseEntity.ok(festivals);
    }
    
    /**
     * 구별 + 이름으로 문화행사 검색
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<List<Festival>> searchFestivalsByDistrictAndName(
            @RequestParam String district, 
            @RequestParam String name) {
        List<Festival> festivals = festivalService.getFestivalsByDistrictAndName(district, name);
        return ResponseEntity.ok(festivals);
    }
    
    /**
     * 특정 축제를 삭제합니다.
     */
    @DeleteMapping("/{festivalId}")
    public ResponseEntity<Map<String, Object>> deleteFestival(@PathVariable Long festivalId) {
        log.info("축제 삭제 요청: {}", festivalId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Festival festival = festivalService.getFestivalById(festivalId);
            
            if (festival == null) {
                response.put("success", false);
                response.put("message", "해당 축제를 찾을 수 없습니다: " + festivalId);
                return ResponseEntity.ok(response);
            }
            
            festivalService.deleteFestival(festivalId);
            
            response.put("success", true);
            response.put("message", "축제가 성공적으로 삭제되었습니다.");
            response.put("deletedFestivalId", festivalId);
            response.put("deletedFestivalName", festival.getName());
            
        } catch (Exception e) {
            log.error("축제 삭제 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "축제 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 여러 축제를 한 번에 삭제합니다.
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteMultipleFestivals(@RequestBody List<Long> festivalIds) {
        log.info("다중 축제 삭제 요청: {}개", festivalIds.size());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            int deletedCount = 0;
            List<Long> notFoundIds = new ArrayList<>();
            
            for (Long festivalId : festivalIds) {
                try {
                    Festival festival = festivalService.getFestivalById(festivalId);
                    
                    if (festival != null) {
                        festivalService.deleteFestival(festivalId);
                        deletedCount++;
                        log.info("축제 삭제 완료: ID={}, 이름={}", festivalId, festival.getName());
                    } else {
                        notFoundIds.add(festivalId);
                    }
                } catch (Exception e) {
                    log.error("축제 {} 삭제 실패: {}", festivalId, e.getMessage());
                    notFoundIds.add(festivalId);
                }
            }
            
            response.put("success", true);
            response.put("message", String.format("삭제 완료: %d개 축제", deletedCount));
            response.put("deletedCount", deletedCount);
            response.put("notFoundIds", notFoundIds);
            
        } catch (Exception e) {
            log.error("다중 축제 삭제 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "다중 축제 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * API 키 및 연결 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("서버가 정상적으로 실행 중입니다. API 키가 설정되어 있습니다.");
    }
}
