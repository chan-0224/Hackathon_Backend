package com.example.hackathon.service;

import com.example.hackathon.dto.CulturalEventApiResponse;
import com.example.hackathon.dto.CulturalEventInfo;
import com.example.hackathon.dto.CulturalEventRow;
import com.example.hackathon.entity.Festival;
import com.example.hackathon.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class FestivalService {
    
    private final FestivalRepository festivalRepository;
    private final WebClient webClient;
    private final FestivalAIService festivalAIService;
    
    @Value("${seoul.culture.api.key}")
    private String apiKey;
    
    @PostConstruct
    public void init() {
        log.info("FestivalService 초기화 - API 키: {}", apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "null");
    }
    
    /**
     * 서울시 문화행사 API에서 데이터를 가져와 데이터베이스에 저장
     * 페이징 처리를 통해 모든 데이터를 수집
     */
    public void fetchAndSaveFestivals() {
        log.info("서울시 문화행사 데이터 수집 시작");
        
        try {
            int startIndex = 1;
            int pageSize = "sample".equals(apiKey) ? 5 : 1000; // 샘플 키는 5건씩만 가능
            int totalCount = 0;
            int savedCount = 0;
            
            do {
                log.info("API 호출: {} ~ {}", startIndex, startIndex + pageSize - 1);
                
                // API 호출
                CulturalEventApiResponse response = callApi(startIndex, startIndex + pageSize - 1);
                
                if (response != null && response.getCulturalEventInfo() != null) {
                    CulturalEventInfo eventInfo = response.getCulturalEventInfo();
                    totalCount = eventInfo.getListTotalCount();
                    
                    // 데이터 저장
                    int saved = saveFestivals(eventInfo.getRow());
                    savedCount += saved;
                    
                    log.info("저장된 데이터: {}건 (총 {}건 중)", saved, totalCount);
                    
                    // 더 이상 데이터가 없으면 중단
                    if (eventInfo.getRow() == null || eventInfo.getRow().size() < pageSize) {
                        log.info("마지막 페이지에 도달했습니다.");
                        break;
                    }
                }
                
                startIndex += pageSize;
                
            } while (startIndex <= totalCount);
            
            log.info("서울시 문화행사 데이터 수집 완료. 총 {}건 저장됨", savedCount);
            
        } catch (Exception e) {
            log.error("서울시 문화행사 데이터 수집 중 오류 발생", e);
            throw new RuntimeException("문화행사 데이터 수집 실패", e);
        }
    }
    
    /**
     * 특정 '구'의 행사 목록 조회
     */
    public List<Festival> getFestivalsByDistrict(String district) {
        log.info("{} 구 문화행사 조회", district);
        return festivalRepository.findByDistrict(district);
    }
    
    /**
     * 전체 행사 목록 조회
     */
    public List<Festival> getAllFestivals() {
        log.info("전체 문화행사 조회");
        return festivalRepository.findAll();
    }
    
    /**
     * 이름으로 행사 검색
     */
    public List<Festival> getFestivalsByName(String name) {
        log.info("문화행사 이름 검색: {}", name);
        return festivalRepository.findByNameContaining(name);
    }
    
    /**
     * 구별 + 이름으로 행사 검색
     */
    public List<Festival> getFestivalsByDistrictAndName(String district, String name) {
        log.info("{} 구, {} 이름으로 문화행사 검색", district, name);
        return festivalRepository.findByDistrictAndNameContaining(district, name);
    }
    
    /**
     * ID로 특정 문화행사 조회
     */
    public Festival getFestivalById(Long id) {
        log.info("문화행사 ID 조회: {}", id);
        return festivalRepository.findById(id).orElse(null);
    }
    
    /**
     * API 호출 (재시도 로직 포함)
     */
    @Retryable(value = {WebClientResponseException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private CulturalEventApiResponse callApi(int startIndex, int endIndex) {
        // 서울시 API JSON 포맷 명시 및 올바른 서비스명 사용
        String url = String.format("/%s/json/culturalEventInfo/%d/%d/", apiKey, startIndex, endIndex);
        
        log.info("API 호출 URL: {}", url);
        
        try {
            // 응답을 문자열로 받아서 확인
            String responseBody = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            log.info("API 응답 길이: {} 문자", responseBody.length());
            log.info("API 응답 시작 부분: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
            
            // 오류 응답인지 확인
            if (responseBody.contains("ERROR")) {
                log.error("API 오류 응답: {}", responseBody);
                throw new RuntimeException("API 오류: " + responseBody);
            }
            
            // JSON 파싱 시도
            CulturalEventApiResponse response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(CulturalEventApiResponse.class)
                .block();
            
            log.info("API 응답 성공: {}", response != null ? "응답 받음" : "응답 없음");
            if (response != null && response.getCulturalEventInfo() != null) {
                log.info("culturalEventInfo 존재: {}", response.getCulturalEventInfo().getListTotalCount());
                log.info("row 데이터 개수: {}", response.getCulturalEventInfo().getRow() != null ? response.getCulturalEventInfo().getRow().size() : 0);
            } else {
                log.warn("culturalEventInfo가 null입니다.");
            }
            return response;
            
        } catch (WebClientResponseException e) {
            log.error("API 호출 실패 - HTTP 상태: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("API 호출 중 예외 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * API 응답 데이터를 Festival 엔티티로 변환하여 저장
     * 중복 데이터는 제외하고 저장
     */
    private int saveFestivals(List<CulturalEventRow> rows) {
        if (rows == null || rows.isEmpty()) {
            log.warn("저장할 데이터가 없습니다.");
            return 0;
        }
        
        log.info("변환할 데이터: {}건", rows.size());
        
        List<Festival> newFestivals = rows.stream()
            .map(this::convertToFestival)
            .filter(festival -> festival != null && festival.getUniqueKey() != null)
            .filter(festival -> !festivalRepository.existsByUniqueKey(festival.getUniqueKey()))
            .collect(Collectors.toList());
        
        log.info("새로운 문화행사: {}건", newFestivals.size());
        
        if (!newFestivals.isEmpty()) {
            // 하나씩 저장해서 중복 오류 방지
            int savedCount = 0;
            for (Festival festival : newFestivals) {
                try {
                    // 한 번 더 중복 체크
                    if (!festivalRepository.existsByUniqueKey(festival.getUniqueKey())) {
                        // AI 요약 및 태그 생성
                        generateAIForFestival(festival);
                        
                        festivalRepository.save(festival);
                        savedCount++;
                    } else {
                        log.debug("중복 데이터 건너뜀: {}", festival.getUniqueKey());
                    }
                } catch (Exception e) {
                    log.warn("데이터 저장 실패 (중복일 가능성): {}", festival.getUniqueKey(), e);
                }
            }
            log.info("새로운 문화행사 {}건 저장됨", savedCount);
            return savedCount;
        }
        
        return 0;
    }
    
    /**
     * API 응답 데이터를 Festival 엔티티로 변환
     */
    private Festival convertToFestival(CulturalEventRow row) {
        try {
            Festival festival = new Festival();
            festival.setName(row.getTitle());
            festival.setDate(row.getDate());
            festival.setDistrict(row.getGuname());
            festival.setPlace(row.getPlace());
            
            // ORG_LINK를 우선 사용하고, 없으면 HMPG_ADDR 사용
            String link = (row.getOrgLink() != null && !row.getOrgLink().trim().isEmpty()) 
                ? row.getOrgLink() 
                : row.getHmpgAddr();
            festival.setLink(link);
            
            // 고유 키 생성 (TITLE + DATE + PLACE + GUNAME 조합)
            // uniqueKey 생성 (해시 기반으로 짧고 안전하게)
            String rawKey = String.format("%s_%s_%s_%s", 
                row.getTitle(), row.getDate(), row.getPlace(), row.getGuname());
            // 해시로 변환해서 길이 제한
            String uniqueKey = String.valueOf(rawKey.hashCode());
            festival.setUniqueKey(uniqueKey);
            
            return festival;
            
        } catch (Exception e) {
            log.warn("문화행사 데이터 변환 실패: {}", row, e);
            return null;
        }
    }
    
    /**
     * 축제에 대한 AI 요약 및 태그를 생성합니다.
     */
    private void generateAIForFestival(Festival festival) {
        try {
            log.info("축제 AI 생성 시작: {}", festival.getName());
            
            // AI 요약 생성
            String aiSummary = festivalAIService.generateAISummary(festival);
            if (aiSummary != null) {
                festival.setAiSummary(aiSummary);
                log.info("AI 요약 생성 완료: {}", festival.getName());
            } else {
                log.warn("AI 요약 생성 실패: {}", festival.getName());
            }
            
            // 분위기 태그 생성
            String moodTags = festivalAIService.generateMoodTags(festival);
            if (moodTags != null) {
                festival.setMoodTags(moodTags);
                log.info("분위기 태그 생성 완료: {}", festival.getName());
            } else {
                log.warn("분위기 태그 생성 실패: {}", festival.getName());
            }
            
        } catch (Exception e) {
            log.error("축제 AI 생성 중 오류 발생: {} - {}", festival.getName(), e.getMessage());
            // AI 생성 실패해도 기본 데이터는 저장
        }
    }
    
    /**
     * 기존 축제 데이터에 AI 요약 및 태그를 생성합니다.
     */
    public void generateAIForExistingFestivals() {
        log.info("기존 축제 데이터 AI 생성 시작");
        
        List<Festival> festivals = festivalRepository.findAll();
        int processedCount = 0;
        int successCount = 0;
        
        for (Festival festival : festivals) {
            try {
                processedCount++;
                
                // AI 요약이 없으면 생성
                if (festival.getAiSummary() == null || festival.getAiSummary().trim().isEmpty()) {
                    String aiSummary = festivalAIService.generateAISummary(festival);
                    if (aiSummary != null) {
                        festival.setAiSummary(aiSummary);
                        log.info("기존 축제 AI 요약 생성 완료: {} ({}/{})", festival.getName(), processedCount, festivals.size());
                    }
                }
                
                // 분위기 태그가 없으면 생성
                if (festival.getMoodTags() == null || festival.getMoodTags().trim().isEmpty()) {
                    String moodTags = festivalAIService.generateMoodTags(festival);
                    if (moodTags != null) {
                        festival.setMoodTags(moodTags);
                        log.info("기존 축제 분위기 태그 생성 완료: {} ({}/{})", festival.getName(), processedCount, festivals.size());
                    }
                }
                
                // 변경사항이 있으면 저장
                if (festival.getAiSummary() != null || festival.getMoodTags() != null) {
                    festivalRepository.save(festival);
                    successCount++;
                }
                
                // API 호출 제한을 위한 딜레이
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.error("기존 축제 AI 생성 실패: {} - {}", festival.getName(), e.getMessage());
            }
        }
        
        log.info("기존 축제 데이터 AI 생성 완료: {}건 처리, {}건 성공", processedCount, successCount);
    }
    
    /**
     * 매일 새벽 2시에 자동으로 문화행사 데이터 수집
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledFetchAndSaveFestivals() {
        log.info("스케줄된 문화행사 데이터 수집 시작");
        fetchAndSaveFestivals();
    }

    /**
     * 태그 기반 추천 문화행사 조회 (페이징 포함)
     */
    public Map<String, Object> getRecommendedFestivals(String tags, int page, int size) {
        log.info("태그 기반 추천 문화행사 조회 - 태그: {}, 페이지: {}, 크기: {}", tags, page, size);
        
        try {
            // 태그 파라미터 처리
            String processedTag = processTag(tags);
            
            if (processedTag == null || processedTag.isEmpty()) {
                log.info("유효한 태그가 없어 전체 문화행사를 반환합니다.");
                Pageable pageable = PageRequest.of(page, size);
                Page<Festival> allFestivals = festivalRepository.findAll(pageable);
                
                return Map.of(
                    "success", true,
                    "data", allFestivals.getContent(),
                    "totalCount", allFestivals.getTotalElements(),
                    "totalPages", allFestivals.getTotalPages(),
                    "currentPage", page,
                    "pageSize", size
                );
            }
            
            // 페이징 설정
            Pageable pageable = PageRequest.of(page, size);
            
            // 추천 문화행사 조회 (첫 번째 태그만 사용)
            Page<Festival> recommendedFestivals = festivalRepository.findFestivalsByTagWithPaging(processedTag, pageable);
            
            log.info("추천 문화행사 조회 완료 - 총 {}건, 현재 페이지 {}건", 
                recommendedFestivals.getTotalElements(), recommendedFestivals.getContent().size());
            
            return Map.of(
                "success", true,
                "data", recommendedFestivals.getContent(),
                "totalCount", recommendedFestivals.getTotalElements(),
                "totalPages", recommendedFestivals.getTotalPages(),
                "currentPage", page,
                "pageSize", size,
                "requestedTag", processedTag
            );
            
        } catch (Exception e) {
            log.error("태그 기반 추천 문화행사 조회 중 오류 발생", e);
            return Map.of(
                "success", false,
                "error", "추천 문화행사 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }
    
    /**
     * 태그 파라미터 처리 및 유효성 검사 (단일 태그)
     */
    private String processTag(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return null;
        }
        
        // 쉼표로 분리하고 첫 번째 태그만 사용
        String[] tagArray = tags.split(",");
        if (tagArray.length > 0) {
            String firstTag = tagArray[0].trim();
            if (!firstTag.isEmpty()) {
                log.debug("처리된 태그: {}", firstTag);
                return firstTag;
            }
        }
        
        return null;
    }
}
