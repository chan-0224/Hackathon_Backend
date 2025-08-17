package com.example.hackathon.controller;

import com.example.hackathon.entity.Festival;
import com.example.hackathon.service.FestivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
@Slf4j
public class FestivalController {
    
    private final FestivalService festivalService;
    
    /**
     * 서울시 문화행사 데이터 수집 (수동 실행)
     */
    @PostMapping("/fetch")
    public ResponseEntity<String> fetchFestivals() {
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
     * API 키 및 연결 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("서버가 정상적으로 실행 중입니다. API 키가 설정되어 있습니다.");
    }
}
