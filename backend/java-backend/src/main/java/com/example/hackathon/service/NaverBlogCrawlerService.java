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
     * 축제 이름으로 네이버 블로그 리뷰를 크롤링합니다. (하나의 리뷰만)
     * 
     * @param festivalName 축제 이름 (검색 키워드)
     * @param maxReviews 크롤링할 최대 리뷰 수 (사용하지 않음, 항상 1개만)
     * @return 크롤링된 리뷰 목록
     */
    public List<Review> crawlFestivalReviews(String festivalName, int maxReviews) {
        log.info("축제 리뷰 크롤링 시작: {} (1개만)", festivalName);
        
        // 크롤링 활성화 확인
        if (!crawlerConfig.isEnabled()) {
            log.warn("크롤링이 비활성화되어 있습니다.");
            return new ArrayList<>();
        }
        
        // 일일 요청 수 제한 확인
        checkDailyLimit();
        
        List<Review> reviews = new ArrayList<>();
        
        try {
            // 간단하게 하나의 검색 쿼리만 사용
            String searchQuery = festivalName + " 후기";
            log.info("검색 쿼리: {}", searchQuery);
            
            // 1. 네이버 블로그 검색 결과에서 첫 번째 URL만 가져오기
            List<String> blogUrls = crawlSearchResults(searchQuery, 1);
            
            if (blogUrls.isEmpty()) {
                log.warn("검색 결과가 없습니다.");
                return reviews;
            }
            
            // 2. 첫 번째 블로그 URL만 크롤링
            String blogUrl = blogUrls.get(0);
            log.info("크롤링할 블로그 URL: {}", blogUrl);
            
            try {
                Review review = crawlBlogContent(blogUrl, festivalName);
                if (review != null) {
                    reviews.add(review);
                    log.info("리뷰 크롤링 완료: 1개");
                }
                
            } catch (Exception e) {
                log.error("블로그 크롤링 실패: {} - {}", blogUrl, e.getMessage());
                // 실패한 리뷰 저장
                saveFailedReview(blogUrl, festivalName, e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("크롤링 중 오류 발생: {}", e.getMessage());
        }
        
        log.info("크롤링 완료: {} (성공: {}개)", festivalName, reviews.size());
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
        
        // 1. 기본 선택자로 시도
        Elements linkElements = doc.select("a.total_tit, a.link_tit, .total_tit a, .link_tit a");
        
        for (Element link : linkElements) {
            if (urls.size() >= maxResults) break;
            
            String href = link.attr("href");
            if (href.startsWith("https://blog.naver.com/")) {
                urls.add(href);
            }
        }
        
        // 2. 모든 링크 확인
        Elements allLinks = doc.select("a[href]");
        
        int blogLinkCount = 0;
        for (Element link : allLinks) {
            String href = link.attr("href");
            if (href.contains("blog.naver.com")) {
                blogLinkCount++;
            }
        }
        
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
    private Review crawlBlogContent(String blogUrl, String festivalName) throws IOException {
        // 1. 블로그 메인 페이지에서 iframe URL 찾기
        Document blogDoc = getDocumentWithRetry(blogUrl);
        if (blogDoc == null) {
            log.error("블로그 메인 페이지를 가져올 수 없습니다: {}", blogUrl);
            return null;
        }
        
        // iframe 찾기 (여러 방법 시도)
        Element iframe = blogDoc.selectFirst("iframe#mainFrame");
        if (iframe == null) {
            // 대체 방법들 시도
            iframe = blogDoc.selectFirst("iframe[name='mainFrame']");
        }
        if (iframe == null) {
            iframe = blogDoc.selectFirst("iframe[src*='PostView.naver']");
        }
        
        if (iframe == null) {
            log.error("iframe을 찾을 수 없습니다: {}", blogUrl);
            return null;
        }
        
        String iframeSrc = iframe.attr("src");
        
        // iframe src가 상대 경로인 경우 절대 경로로 변환
        String iframeUrl;
        if (iframeSrc.startsWith("/")) {
            iframeUrl = "https://blog.naver.com" + iframeSrc;
        } else if (iframeSrc.startsWith("http")) {
            iframeUrl = iframeSrc;
        } else {
            iframeUrl = "https://blog.naver.com/" + iframeSrc;
        }
        
        log.debug("최종 iframe URL: {}", iframeUrl);
        
        // 2. iframe 내부의 실제 블로그 내용 크롤링
        Document postDoc = getDocumentWithRetry(iframeUrl);
        if (postDoc == null) {
            log.error("iframe 내용을 가져올 수 없습니다: {}", iframeUrl);
            return null;
        }
        
        log.debug("iframe 페이지 제목: {}", postDoc.title());
        log.debug("iframe 페이지 URL: {}", postDoc.baseUri());
        
        // 차단 감지
        if (isBlocked(postDoc)) {
            log.error("블로그 포스트에서 차단되었습니다: {}", iframeUrl);
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
            String content = extractContent(doc).toString(); // extractContent 메서드가 String을 반환하도록 변경
            
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
        log.debug("=== 제목 추출 시작 ===");
        log.debug("페이지 제목: {}", doc.title());
        
        // 네이버 블로그 PostView.naver 페이지의 실제 제목 선택자들 (2024-2025)
        String[] titleSelectors = {
            // 최신 네이버 블로그 제목 선택자들
            "div.se-title",
            "h3.se-title", 
            ".se-title",
            "div.se-module-title",
            "div.se-module-se-title",
            "div.se-module-se-main-title",
            "div.se-module-se-main-container h1",
            "div.se-module-se-main-container h2",
            "div.se-module-se-main-container h3",
            "div.se-module-se-main-container .se-title",
            "div.se-module-se-main-container .title",
            "div.se-module-se-main-container .post-title",
            "div.se-module-se-main-container .entry-title",
            "div.se-module-se-main-container .blog-title",
            "div.se-module-se-main-container .article-title",
            "div.se-module-se-main-container .content-title",
            "div.se-module-se-main-container .main-title",
            "div.se-module-se-main-container .head-title",
            "div.se-module-se-main-container .subject",
            "div.se-module-se-main-container .subject-title",
            "div.se-module-se-main-container .post-subject",
            "div.se-module-se-main-container .entry-subject",
            "div.se-module-se-main-container .blog-subject",
            "div.se-module-se-main-container .article-subject",
            "div.se-module-se-main-container .content-subject",
            "div.se-module-se-main-container .main-subject",
            "div.se-module-se-main-container .head-subject",
            // 추가 최신 선택자들
            "div.se-viewer-title",
            "div.se-viewer-main-title",
            "div.se-viewer-main-container h1",
            "div.se-viewer-main-container h2",
            "div.se-viewer-main-container h3",
            "div.se-viewer-main-container .se-title",
            "div.se-viewer-main-container .title",
            "div.se-viewer-main-container .post-title",
            "div.se-viewer-main-container .entry-title",
            "div.se-viewer-main-container .blog-title",
            "div.se-viewer-main-container .article-title",
            "div.se-viewer-main-container .content-title",
            "div.se-viewer-main-container .main-title",
            "div.se-viewer-main-container .head-title",
            "div.se-viewer-main-container .subject",
            "div.se-viewer-main-container .subject-title",
            "div.se-viewer-main-container .post-subject",
            "div.se-viewer-main-container .entry-subject",
            "div.se-viewer-main-container .blog-subject",
            "div.se-viewer-main-container .article-subject",
            "div.se-viewer-main-container .content-subject",
            "div.se-viewer-main-container .main-subject",
            "div.se-viewer-main-container .head-subject",
            // 기존 선택자들
            "h1", "h2", "h3", ".title", ".post-title", ".entry-title"
        };
        
        for (String selector : titleSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String title = element.text().trim();
                if (!title.isEmpty() && title.length() > 2) {
                    log.debug("제목 추출 성공 (선택자: {}): {}", selector, title);
                    return title;
                }
            }
        }
        
        // 선택자로 찾지 못한 경우, 페이지 제목에서 추출 시도 (더 정교하게)
        String pageTitle = doc.title();
        if (pageTitle != null) {
            // "블로그", "네이버" 등 불필요한 텍스트 제거
            String cleanTitle = pageTitle.replace("블로그", "").replace("네이버", "").trim();
            if (cleanTitle.contains(":")) {
                String extractedTitle = cleanTitle.split(":")[0].trim();
                if (!extractedTitle.isEmpty() && extractedTitle.length() > 2) {
                    log.debug("페이지 제목에서 제목 추출: {}", extractedTitle);
                    return extractedTitle;
                }
            } else if (cleanTitle.length() > 2) {
                log.debug("페이지 제목에서 제목 추출: {}", cleanTitle);
                return cleanTitle;
            }
        }
        
        // 본문 내용의 첫 번째 문단에서 제목 추출 시도
        String content = extractContent(doc);
        if (content != null && content.length() > 20) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.length() > 5 && trimmedLine.length() < 100 && 
                    !trimmedLine.contains("네이버") && !trimmedLine.contains("블로그")) {
                    log.debug("본문 첫 문단에서 제목 추출: {}", trimmedLine);
                    return trimmedLine;
                }
            }
        }
        
        log.warn("모든 방법으로도 제목을 추출할 수 없습니다.");
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
        log.debug("=== 본문 내용 추출 시작 ===");
        log.debug("페이지 제목: {}", doc.title());
        log.debug("페이지 URL: {}", doc.baseUri());
        
        // HTML 내용 일부 출력 (디버깅용)
        String html = doc.html();
        log.debug("전체 HTML 길이: {}자", html.length());
        if (html.length() > 1000) {
            log.debug("HTML 미리보기 (처음 1000자): {}", html.substring(0, 1000));
        }
        
        // 네이버 블로그 PostView.naver 페이지의 실제 본문 선택자들 (2024-2025)
        String[] contentSelectors = {
            // 최신 네이버 블로그 본문 선택자들 (우선순위 순서)
            "div.se-main-container",
            "div.se-component-content", 
            "div.se-text",
            "div.se-component",
            "div.se-section",
            "div.se-module",
            "div.se-module-text",
            "div.se-module-content",
            "div.se-module-body",
            "div.se-module-inner",
            "div.se-module-wrap",
            "div.se-module-se",
            "div.se-module-se-text",
            "div.se-module-se-content",
            "div.se-module-se-body",
            "div.se-module-se-inner",
            "div.se-module-se-wrap",
            "div.se-module-se-container",
            "div.se-module-se-main",
            "div.se-module-se-main-container",
            "div.se-module-se-main-content",
            "div.se-module-se-main-body",
            "div.se-module-se-main-inner",
            "div.se-module-se-main-wrap",
            "div.se-module-se-main-container",
            // 추가 최신 선택자들
            "div.se-viewer",
            "div.se-viewer-content",
            "div.se-viewer-body",
            "div.se-viewer-inner",
            "div.se-viewer-wrap",
            "div.se-viewer-container",
            "div.se-viewer-main",
            "div.se-viewer-main-content",
            "div.se-viewer-main-body",
            "div.se-viewer-main-inner",
            "div.se-viewer-main-wrap",
            "div.se-viewer-main-container",
            "div.se-post",
            "div.se-post-content",
            "div.se-post-body",
            "div.se-post-inner",
            "div.se-post-wrap",
            "div.se-post-container",
            "div.se-entry",
            "div.se-entry-content",
            "div.se-entry-body",
            "div.se-entry-inner",
            "div.se-entry-wrap",
            "div.se-entry-container",
            // 본문 특화 선택자들
            "div.se-main-container div.se-component",
            "div.se-main-container div.se-component-content",
            "div.se-main-container div.se-text",
            "div.se-main-container div.se-section",
            "div.se-main-container div.se-module",
            "div.se-main-container div.se-module-text",
            "div.se-main-container div.se-module-content",
            "div.se-main-container div.se-module-body",
            "div.se-main-container div.se-module-inner",
            "div.se-main-container div.se-module-wrap",
            "div.se-main-container div.se-module-se",
            "div.se-main-container div.se-module-se-text",
            "div.se-main-container div.se-module-se-content",
            "div.se-main-container div.se-module-se-body",
            "div.se-main-container div.se-module-se-inner",
            "div.se-main-container div.se-module-se-wrap",
            "div.se-main-container div.se-module-se-container",
            "div.se-main-container div.se-module-se-main",
            "div.se-main-container div.se-module-se-main-container",
            "div.se-main-container div.se-module-se-main-content",
            "div.se-main-container div.se-module-se-main-body",
            "div.se-main-container div.se-module-se-main-inner",
            "div.se-main-container div.se-module-se-main-wrap",
            "div.se-main-container div.se-module-se-main-container",
            // 기존 선택자들
            "div.post_content",
            "div.entry-content",
            "div.content",
            "div.post-body",
            "div.entry-body",
            "div.body",
            "div.main",
            "div.main-content",
            "div.main-body",
            "div.main-inner",
            "div.main-wrap",
            "div.main-container",
            "div.article",
            "div.article-content",
            "div.article-body",
            "div.article-inner",
            "div.article-wrap",
            "div.article-container",
            "div.post",
            "div.post-content",
            "div.post-body",
            "div.post-inner",
            "div.post-wrap",
            "div.post-container",
            "div.entry",
            "div.entry-content",
            "div.entry-body",
            "div.entry-inner",
            "div.entry-wrap",
            "div.entry-container"
        };
        
        StringBuilder content = new StringBuilder();
        boolean contentFound = false;
        Set<String> extractedTexts = new HashSet<>(); // 중복 방지용
        
        for (String selector : contentSelectors) {
            if (contentFound) break; // 이미 내용을 찾았으면 중단
            
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                log.debug("선택자 '{}'로 {}개 요소 발견", selector, elements.size());
                
                for (Element element : elements) {
                    String text = element.text().trim();
                    if (!text.isEmpty() && text.length() > 20 && !extractedTexts.contains(text)) { // 중복 방지
                        content.append(text).append("\n\n");
                        extractedTexts.add(text);
                        contentFound = true;
                        log.debug("내용 추출 성공 (선택자: {}): {}자", selector, text.length());
                        log.debug("추출된 텍스트 미리보기: {}", text.length() > 100 ? text.substring(0, 100) + "..." : text);
                        break; // 첫 번째 의미있는 텍스트만 추출
                    }
                }
            }
        }
        
        // 선택자로 찾지 못한 경우, 전체 텍스트에서 의미있는 부분 추출
        if (!contentFound) {
            log.warn("기본 선택자로 블로그 내용을 찾지 못했습니다. 다른 방식으로 시도합니다.");
            
            // p 태그들에서 텍스트 추출
            Elements paragraphs = doc.select("p");
            log.debug("p 태그 {}개 발견", paragraphs.size());
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (text.length() > 20) { // 긴 문단만
                    content.append(text).append("\n\n");
                    contentFound = true;
                    log.debug("p 태그에서 내용 추출: {}자", text.length());
                }
            }
            
            // 여전히 못 찾은 경우, div 태그들에서 텍스트 추출
            if (!contentFound) {
                Elements divs = doc.select("div");
                log.debug("div 태그 {}개 발견", divs.size());
                for (Element div : divs) {
                    String text = div.text().trim();
                    if (text.length() > 50 && !text.contains("네이버") && !text.contains("블로그")) {
                        content.append(text).append("\n\n");
                        contentFound = true;
                        log.debug("div 태그에서 내용 추출: {}자", text.length());
                        break; // 첫 번째 의미있는 div만
                    }
                }
            }
            
            // 마지막 방법: span 태그들에서 텍스트 추출
            if (!contentFound) {
                Elements spans = doc.select("span");
                log.debug("span 태그 {}개 발견", spans.size());
                for (Element span : spans) {
                    String text = span.text().trim();
                    if (text.length() > 30 && !text.contains("네이버") && !text.contains("블로그")) {
                        content.append(text).append("\n\n");
                        contentFound = true;
                        log.debug("span 태그에서 내용 추출: {}자", text.length());
                    }
                }
            }
            
            // 최후의 방법: 전체 텍스트에서 의미있는 부분 추출
            if (!contentFound) {
                log.warn("모든 선택자로도 내용을 찾지 못했습니다. 전체 텍스트에서 추출을 시도합니다.");
                String fullText = doc.text();
                if (fullText.length() > 100) {
                    // 네이버 관련 텍스트 제거하고 의미있는 부분만 추출
                    String cleanedText = fullText
                        .replaceAll("네이버.*?블로그", "")
                        .replaceAll("로그인.*?회원가입", "")
                        .replaceAll("검색.*?결과", "")
                        .trim();
                    
                    if (cleanedText.length() > 50) {
                        content.append(cleanedText);
                        contentFound = true;
                        log.debug("전체 텍스트에서 내용 추출: {}자", cleanedText.length());
                    }
                }
            }
        }
        
        // 텍스트 정제
        String cleanedContent = content.toString()
                .replaceAll("\\s+", " ")
                .trim();
        
        if (cleanedContent.isEmpty()) {
            log.error("모든 방법으로도 내용을 추출할 수 없습니다.");
            return "내용을 추출할 수 없습니다.";
        }
        
        log.debug("최종 추출된 내용 길이: {}자", cleanedContent.length());
        log.debug("최종 내용 미리보기: {}", cleanedContent.length() > 200 ? cleanedContent.substring(0, 200) + "..." : cleanedContent);
        
        return cleanedContent;
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
