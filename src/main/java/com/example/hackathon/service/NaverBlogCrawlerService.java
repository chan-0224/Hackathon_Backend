package com.example.hackathon.service;

import com.example.hackathon.config.CrawlerConfig;
import com.example.hackathon.entity.Review;
import com.example.hackathon.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 네이버 블로그 크롤링 서비스
 * 안전하고 효율적인 크롤링을 위한 다양한 보호 장치를 포함합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NaverBlogCrawlerService {
    
    private final CrawlerConfig crawlerConfig;
    private final ReviewRepository reviewRepository;
    
    // 일일 요청 수 추적
    private final AtomicInteger dailyRequestCount = new AtomicInteger(0);
    private LocalDateTime lastResetDate = LocalDateTime.now();
    
    /**
     * 축제 이름으로 네이버 블로그 리뷰를 크롤링합니다.
     * 
     * @param festivalName 축제 이름 (검색 키워드)
     * @param maxReviews 크롤링할 최대 리뷰 수
     * @return 크롤링된 리뷰 목록
     */
    public List<Review> crawlFestivalReviews(String festivalName, int maxReviews) {
        log.info("축제 리뷰 크롤링 시작: {} (최대 {}개)", festivalName, maxReviews);
        
        // 크롤링 활성화 확인
        if (!crawlerConfig.isEnabled()) {
            log.warn("크롤링이 비활성화되어 있습니다.");
            return new ArrayList<>();
        }
        
        // 일일 요청 수 제한 확인
        checkDailyLimit();
        
        List<Review> reviews = new ArrayList<>();
        
        // 다양한 검색 쿼리 시도
        List<String> searchQueries = new ArrayList<>();
        searchQueries.add(festivalName + " 후기");
        searchQueries.add(festivalName + " 리뷰");
        searchQueries.add(festivalName + " 다녀왔어요");
        searchQueries.add(festivalName + " 가봤어요");
        
        // 축제 이름에 공백이 있으면 분리해서도 검색
        if (festivalName.contains(" ")) {
            String[] words = festivalName.split(" ");
            if (words.length >= 2) {
                searchQueries.add(words[0] + " " + words[1] + " 후기");
                searchQueries.add(words[0] + " " + words[1] + " 리뷰");
            }
        }
        
        log.info("검색할 쿼리들: {}", searchQueries);
        
        try {
            // 1. 다양한 검색 쿼리로 네이버 블로그 검색 결과 페이지 크롤링
            Set<String> allBlogUrls = new HashSet<>(); // 중복 URL 제거
            
            for (String searchQuery : searchQueries) {
                if (allBlogUrls.size() >= maxReviews * 2) break; // 충분한 URL 수집 시 중단
                
                List<String> blogUrls = crawlSearchResults(searchQuery, maxReviews);
                allBlogUrls.addAll(blogUrls);
                log.info("검색 쿼리 '{}'에서 {}개 URL 수집 (총 {}개)", searchQuery, blogUrls.size(), allBlogUrls.size());
                
                // 검색 쿼리 간 안전한 지연
                if (searchQueries.indexOf(searchQuery) < searchQueries.size() - 1) {
                    try {
                        Thread.sleep(3000 + new Random().nextInt(2000)); // 3-5초 지연
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            List<String> blogUrls = new ArrayList<>(allBlogUrls);
            log.info("최종 검색된 블로그 URL 수: {}개", blogUrls.size());
            
            // 2. 각 블로그 글의 본문 크롤링
            for (int i = 0; i < blogUrls.size() && reviews.size() < maxReviews; i++) {
                String blogUrl = blogUrls.get(i);
                
                // 중복 URL 체크
                if (reviewRepository.existsByBlogUrl(blogUrl)) {
                    log.debug("이미 크롤링된 URL 건너뛰기: {}", blogUrl);
                    continue;
                }
                
                try {
                    Review review = crawlBlogPost(blogUrl, festivalName);
                    if (review != null && review.getIsSuccess()) {
                        reviews.add(review);
                        reviewRepository.save(review);
                        log.info("[{}/{}] 리뷰 크롤링 성공: {}", reviews.size(), maxReviews, blogUrl);
                    }
                    
                    // 안전한 지연시간 적용
                    safeDelay();
                    
                } catch (Exception e) {
                    log.error("블로그 크롤링 실패 ({}): {}", blogUrl, e.getMessage());
                    saveFailedReview(blogUrl, festivalName, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("전체 크롤링 프로세스 실패: {}", e.getMessage(), e);
        }
        
        log.info("크롤링 완료: {}개 리뷰 수집", reviews.size());
        return reviews;
    }
    
    /**
     * 네이버 블로그 검색 결과에서 URL 목록을 추출합니다.
     * 여러 페이지를 순차적으로 크롤링하여 더 많은 결과를 수집합니다.
     */
    private List<String> crawlSearchResults(String query, int maxResults) throws IOException {
        List<String> urls = new ArrayList<>();
        String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        
        // 여러 페이지를 순차적으로 크롤링
        int page = 1;
        int maxPages = (maxResults / 10) + 2; // 충분한 페이지 수 확보
        
        log.info("검색 쿼리: '{}', 최대 {}개 결과 수집을 위해 {}페이지까지 크롤링", query, maxResults, maxPages);
        
        while (urls.size() < maxResults && page <= maxPages) {
            String searchUrl = String.format("https://search.naver.com/search.naver?where=blog&query=%s&start=%d", 
                encodedQuery, (page - 1) * 10 + 1);
            
            log.info("페이지 {} 크롤링 중... (현재 {}개 URL 수집됨)", page, urls.size());
            
            Document doc = getDocumentWithRetry(searchUrl);
            if (doc == null) {
                log.warn("페이지 {} 로딩 실패", page);
                page++;
                continue;
            }
            
            // 차단 감지
            if (isBlocked(doc)) {
                log.error("네이버에서 차단되었습니다. 크롤링을 중단합니다.");
                break;
            }
            
            // 현재 페이지에서 블로그 링크 추출
            List<String> pageUrls = extractBlogUrlsFromPage(doc, maxResults - urls.size());
            
            if (pageUrls.isEmpty()) {
                log.info("페이지 {}에서 더 이상 블로그 링크를 찾을 수 없습니다.", page);
                break;
            }
            
            urls.addAll(pageUrls);
            log.info("페이지 {}에서 {}개 URL 추가 (총 {}개)", page, pageUrls.size(), urls.size());
            
            // 페이지 간 안전한 지연
            if (page < maxPages && urls.size() < maxResults) {
                try {
                    Thread.sleep(2000 + new Random().nextInt(3000)); // 2-5초 지연
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            page++;
        }
        
        log.info("검색 결과 크롤링 완료: 총 {}개 URL 수집", urls.size());
        return urls;
    }
    
    /**
     * 단일 페이지에서 블로그 URL을 추출합니다.
     */
    private List<String> extractBlogUrlsFromPage(Document doc, int maxResults) {
        List<String> urls = new ArrayList<>();
        
        log.debug("=== 페이지 링크 추출 디버깅 ===");
        
        // 1. 기본 선택자로 시도
        Elements linkElements = doc.select("a.total_tit, a.link_tit, .total_tit a, .link_tit a");
        log.debug("기본 선택자로 찾은 링크 요소 수: {}", linkElements.size());
        
        for (Element link : linkElements) {
            if (urls.size() >= maxResults) break;
            
            String href = link.attr("href");
            String linkText = link.text().trim();
            log.debug("링크 텍스트: '{}', URL: {}", linkText, href);
            
            if (href.startsWith("https://blog.naver.com/")) {
                urls.add(href);
                log.debug("블로그 URL 추가: {}", href);
            }
        }
        
        // 2. 모든 링크 확인 (디버깅용)
        Elements allLinks = doc.select("a[href]");
        log.debug("페이지의 모든 링크 수: {}", allLinks.size());
        
        int blogLinkCount = 0;
        for (Element link : allLinks) {
            String href = link.attr("href");
            if (href.contains("blog.naver.com")) {
                blogLinkCount++;
                log.debug("네이버 블로그 링크 발견: {}", href);
            }
        }
        log.debug("페이지에서 발견된 네이버 블로그 링크 총 수: {}", blogLinkCount);
        
        // 3. 만약 블로그 링크를 찾지 못했다면 다른 방법 시도
        if (urls.isEmpty()) {
            log.warn("기본 선택자로 블로그 링크를 찾지 못했습니다. 대체 방법을 시도합니다.");
            
            // 모든 링크에서 네이버 블로그 URL 찾기
            Elements blogLinks = doc.select("a[href*='blog.naver.com']");
            for (Element link : blogLinks) {
                if (urls.size() >= maxResults) break;
                
                String href = link.attr("href");
                if (href.startsWith("https://blog.naver.com/")) {
                    urls.add(href);
                    log.debug("대체 방법으로 블로그 URL 추가: {}", href);
                }
            }
        }
        
        return urls;
    }
    
    /**
     * 개별 블로그 글을 크롤링합니다.
     */
    private Review crawlBlogPost(String blogUrl, String festivalName) throws IOException {
        // 1. 블로그 메인 페이지에서 iframe URL 찾기
        Document blogDoc = getDocumentWithRetry(blogUrl);
        if (blogDoc == null) return null;
        
        Element iframe = blogDoc.selectFirst("iframe#mainFrame");
        if (iframe == null) {
            log.warn("iframe을 찾을 수 없습니다: {}", blogUrl);
            return null;
        }
        
        String iframeSrc = iframe.attr("src");
        String iframeUrl = "https://blog.naver.com" + iframeSrc;
        
        // 2. iframe 내부의 실제 블로그 내용 크롤링
        Document postDoc = getDocumentWithRetry(iframeUrl);
        if (postDoc == null) return null;
        
        // 차단 감지
        if (isBlocked(postDoc)) {
            log.error("블로그 포스트에서 차단되었습니다: {}", blogUrl);
            return null;
        }
        
        return extractReviewData(postDoc, blogUrl, festivalName);
    }
    
    /**
     * 블로그 포스트에서 리뷰 데이터를 추출합니다.
     */
    private Review extractReviewData(Document doc, String blogUrl, String festivalName) {
        try {
            // 제목 추출
            String title = extractTitle(doc);
            
            // 작성자 추출
            String author = extractAuthor(doc);
            
            // 본문 내용 추출
            String content = extractContent(doc);
            
            // 작성일 추출
            LocalDateTime postDate = extractPostDate(doc);
            
            return Review.builder()
                    .festivalName(festivalName)
                    .title(title)
                    .blogUrl(blogUrl)
                    .author(author)
                    .content(content)
                    .postDate(postDate)
                    .isSuccess(true)
                    .userAgent(getRandomUserAgent())
                    .crawledAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("데이터 추출 실패: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 제목을 추출합니다.
     */
    private String extractTitle(Document doc) {
        Element titleElement = doc.selectFirst("div.se-title, h3.se-title, .se-title");
        if (titleElement != null) {
            return titleElement.text().trim();
        }
        
        // 대체 선택자들
        String[] titleSelectors = {
            "h1", "h2", "h3", ".title", ".post-title", ".entry-title"
        };
        
        for (String selector : titleSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                return element.text().trim();
            }
        }
        
        return "제목 없음";
    }
    
    /**
     * 작성자를 추출합니다.
     */
    private String extractAuthor(Document doc) {
        Element authorElement = doc.selectFirst("span.se-nickname, .nickname, .author");
        if (authorElement != null) {
            return authorElement.text().trim();
        }
        return "작성자 정보 없음";
    }
    
    /**
     * 본문 내용을 추출합니다.
     */
    private String extractContent(Document doc) {
        // 네이버 블로그의 주요 본문 선택자들
        String[] contentSelectors = {
            "div.se-main-container",
            "div.se-component-content",
            "div.post_content",
            "div.entry-content",
            "div.se-text"
        };
        
        StringBuilder content = new StringBuilder();
        
        for (String selector : contentSelectors) {
            Elements elements = doc.select(selector);
            for (Element element : elements) {
                String text = element.text().trim();
                if (!text.isEmpty()) {
                    content.append(text).append("\n");
                }
            }
        }
        
        // 텍스트 정제
        String cleanedContent = content.toString()
                .replaceAll("\\s+", " ")
                .trim();
        
        return cleanedContent.isEmpty() ? "내용을 추출할 수 없습니다." : cleanedContent;
    }
    
    /**
     * 작성일을 추출합니다.
     */
    private LocalDateTime extractPostDate(Document doc) {
        try {
            Element dateElement = doc.selectFirst("span.se-date, .date, .post-date");
            if (dateElement != null) {
                String dateText = dateElement.text().trim();
                // 간단한 날짜 파싱 (실제로는 더 정교한 파싱 필요)
                return LocalDateTime.now(); // 임시로 현재 시간 반환
            }
        } catch (Exception e) {
            log.debug("작성일 추출 실패: {}", e.getMessage());
        }
        return LocalDateTime.now();
    }
    
    /**
     * 재시도 로직을 포함한 Document 가져오기
     */
    private Document getDocumentWithRetry(String url) {
        for (int attempt = 1; attempt <= crawlerConfig.getMaxRetries(); attempt++) {
            try {
                String userAgent = getRandomUserAgent();
                Document doc = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .timeout(crawlerConfig.getTimeoutSeconds() * 1000)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3")
                        .header("Accept-Encoding", "gzip, deflate")
                        .header("Connection", "keep-alive")
                        .header("Upgrade-Insecure-Requests", "1")
                        .get();
                
                dailyRequestCount.incrementAndGet();
                return doc;
                
            } catch (IOException e) {
                log.warn("요청 실패 (시도 {}/{}): {} - {}", attempt, crawlerConfig.getMaxRetries(), url, e.getMessage());
                
                if (attempt < crawlerConfig.getMaxRetries()) {
                    try {
                        Thread.sleep(crawlerConfig.getRetryDelaySeconds() * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 차단 여부를 확인합니다.
     */
    private boolean isBlocked(Document doc) {
        String html = doc.html().toLowerCase();
        
        // 상세한 디버깅 정보
        log.debug("=== 차단 감지 디버깅 ===");
        log.debug("페이지 HTML 길이: {}", html.length());
        log.debug("페이지 제목: {}", doc.title());
        log.debug("페이지 URL: {}", doc.baseUri());
        
        // HTML 내용 일부 출력 (처음 500자)
        String htmlPreview = html.length() > 500 ? html.substring(0, 500) + "..." : html;
        log.debug("HTML 미리보기: {}", htmlPreview);
        
        // 차단 키워드 체크 (더 정확한 감지)
        for (String keyword : crawlerConfig.getBlockDetectionKeywords()) {
            if (html.contains(keyword.toLowerCase())) {
                log.warn("차단 키워드 감지: '{}'", keyword);
                return true;
            }
        }
        
        // 추가 차단 감지: 페이지가 너무 짧거나 특정 패턴
        if (html.length() < 1000) {
            log.warn("페이지가 너무 짧습니다 (길이: {})", html.length());
            return true;
        }
        
        // 네이버 검색 결과가 없는 경우
        if (html.contains("검색 결과가 없습니다") || html.contains("no search results")) {
            log.warn("검색 결과가 없습니다");
            return true;
        }
        
        // 네이버 접근 제한 페이지 확인
        if (html.contains("접근이 제한되었습니다") || html.contains("access denied")) {
            log.warn("접근이 제한되었습니다");
            return true;
        }
        
        log.debug("차단 감지 없음 - 정상 페이지로 판단");
        return false;
    }
    
    /**
     * 안전한 지연시간을 적용합니다.
     */
    private void safeDelay() {
        try {
            int delaySeconds = new Random().nextInt(
                crawlerConfig.getMaxDelaySeconds() - crawlerConfig.getMinDelaySeconds() + 1
            ) + crawlerConfig.getMinDelaySeconds();
            
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 랜덤 User-Agent를 반환합니다.
     */
    private String getRandomUserAgent() {
        List<String> userAgents = crawlerConfig.getUserAgents();
        return userAgents.get(new Random().nextInt(userAgents.size()));
    }
    
    /**
     * 일일 요청 수 제한을 확인합니다.
     */
    private void checkDailyLimit() {
        LocalDateTime now = LocalDateTime.now();
        
        // 날짜가 바뀌면 카운터 리셋
        if (now.toLocalDate().isAfter(lastResetDate.toLocalDate())) {
            dailyRequestCount.set(0);
            lastResetDate = now;
        }
        
        if (dailyRequestCount.get() >= crawlerConfig.getMaxDailyRequests()) {
            throw new RuntimeException("일일 크롤링 요청 수 제한에 도달했습니다: " + crawlerConfig.getMaxDailyRequests());
        }
    }
    
    /**
     * 실패한 크롤링을 데이터베이스에 저장합니다.
     */
    private void saveFailedReview(String blogUrl, String festivalName, String errorMessage) {
        try {
            Review failedReview = Review.builder()
                    .festivalName(festivalName)
                    .title("크롤링 실패")
                    .blogUrl(blogUrl)
                    .isSuccess(false)
                    .errorMessage(errorMessage)
                    .userAgent(getRandomUserAgent())
                    .crawledAt(LocalDateTime.now())
                    .build();
            
            reviewRepository.save(failedReview);
        } catch (Exception e) {
            log.error("실패한 리뷰 저장 실패: {}", e.getMessage());
        }
    }
}
