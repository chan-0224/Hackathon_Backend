# 🎯 Java Backend API 명세서

## 📋 개요
- **서버 주소**: `http://3.38.210.80:8081`
- **기본 경로**: `/api`
- **Content-Type**: `application/json`
- **인증**: 없음 (API Key는 서버에서 관리)

---

## 🎪 축제 관련 API

### 1. 축제 데이터 수집
```http
POST /api/festivals/fetch
```
**설명**: 서울시 문화행사 API에서 축제 데이터를 수집하여 데이터베이스에 저장

**응답**:
```json
{
  "message": "문화행사 데이터 수집이 완료되었습니다."
}
```

### 2. 전체 축제 목록 조회
```http
GET /api/festivals
```
**설명**: 저장된 모든 축제 데이터 조회

**응답**:
```json
[
  {
    "id": 1,
    "name": "K-핸드메이드페어 2025",
    "date": "2025-12-18~2025-12-21",
    "district": "강남구",
    "place": "서울 삼성동 코엑스 1층 B홀",
    "link": "https://k-handmade.com/",
    "uniqueKey": "103365564",
    "aiSummary": "{\"장소\": \"서울 삼성동 코엑스 1층 B홀\", \"날짜\": \"2025년 12월 18일부터 2025년 12월 21일까지\", \"주요키워드\": \"핸드메이드, 페어, 강남구, 코엑스, 크리에이티브\"}",
    "moodTags": "[\"핸드메이드\", \"전시\", \"체험\", \"실내\", \"무료\"]",
    "createdAt": "2025-08-25 09:25:35",
    "updatedAt": "2025-08-25 09:25:35"
  }
]
```

### 3. 개별 축제 삭제
```http
DELETE /api/festivals/{festivalId}
```
**설명**: 특정 축제 데이터 삭제

**응답**:
```json
{
  "success": true,
  "message": "축제가 성공적으로 삭제되었습니다.",
  "deletedFestivalId": 1,
  "deletedFestivalName": "K-핸드메이드페어 2025"
}
```

### 4. 축제 일괄 삭제
```http
DELETE /api/festivals/batch
Content-Type: application/json

{
  "festivalIds": [1, 2, 3]
}
```
**설명**: 여러 축제 데이터를 한 번에 삭제

**응답**:
```json
{
  "success": true,
  "message": "삭제 완료: 3개 축제",
  "deletedCount": 3,
  "notFoundIds": []
}
```

---

## 🕷️ 크롤링 관련 API

### 1. 축제 리뷰 크롤링
```http
POST /api/crawler/reviews
Content-Type: application/json

{
  "festivalName": "서울장미축제",
  "maxReviews": 5
}
```
**설명**: 네이버 블로그에서 특정 축제의 리뷰를 크롤링

**응답**:
```json
{
  "success": true,
  "message": "크롤링이 완료되었습니다.",
  "crawledCount": 5,
  "savedCount": 5,
  "reviews": [
    {
      "id": 1,
      "festivalName": "서울장미축제",
      "title": "서울장미축제 후기",
      "blogUrl": "https://blog.naver.com/...",
      "author": "블로거",
      "content": "정말 아름다운 축제였어요...",
      "postDate": "2025-05-18 14:30:00",
      "isSuccess": true,
      "crawledAt": "2025-08-25 10:06:34",
      "createdAt": "2025-08-25 10:06:34",
      "updatedAt": "2025-08-25 10:06:34"
    }
  ]
}
```

### 2. 크롤링된 리뷰 목록 조회
```http
GET /api/crawler/reviews
```
**설명**: 크롤링된 모든 리뷰 조회

**응답**:
```json
{
  "success": true,
  "totalCount": 10,
  "page": 0,
  "size": 20,
  "reviews": [
    {
      "id": 1,
      "festivalName": "서울장미축제",
      "title": "서울장미축제 후기",
      "blogUrl": "https://blog.naver.com/...",
      "author": "블로거",
      "content": "정말 아름다운 축제였어요...",
      "postDate": "2025-05-18 14:30:00",
      "isSuccess": true,
      "crawledAt": "2025-08-25 10:06:34",
      "createdAt": "2025-08-25 10:06:34",
      "updatedAt": "2025-08-25 10:06:34"
    }
  ]
}
```

### 3. 개별 리뷰 삭제
```http
DELETE /api/crawler/reviews/{reviewId}
```
**설명**: 특정 리뷰와 관련 분석 데이터 삭제

**응답**:
```json
{
  "success": true,
  "message": "리뷰와 관련 분석이 성공적으로 삭제되었습니다.",
  "deletedReviewId": 1,
  "deletedAnalysisCount": 1
}
```

### 4. 리뷰 일괄 삭제
```http
DELETE /api/crawler/reviews/batch
Content-Type: application/json

{
  "reviewIds": [1, 2, 3]
}
```
**설명**: 여러 리뷰와 관련 분석 데이터를 한 번에 삭제

**응답**:
```json
{
  "success": true,
  "message": "삭제 완료: 3개 리뷰, 2개 분석 데이터",
  "deletedCount": 3,
  "analysisDeletedCount": 2,
  "notFoundIds": []
}
```

### 5. 특정 축제 리뷰 삭제
```http
DELETE /api/crawler/reviews/festival/{festivalName}
```
**설명**: 특정 축제의 모든 리뷰와 분석 데이터 삭제

**응답**:
```json
{
  "success": true,
  "message": "축제 '서울장미축제'의 5개 리뷰와 3개 분석 데이터가 삭제되었습니다.",
  "festivalName": "서울장미축제",
  "deletedCount": 5,
  "analysisDeletedCount": 3
}
```

### 6. 크롤링 통계 조회
```http
GET /api/crawler/stats
```
**설명**: 크롤링된 데이터 통계 정보 조회

**응답**:
```json
{
  "success": true,
  "totalReviews": 10,
  "successfulReviews": 8,
  "failedReviews": 2,
  "successRate": 80.0,
  "totalAnalyses": 8,
  "successfulAnalyses": 6,
  "failedAnalyses": 2,
  "analysisSuccessRate": 75.0,
  "festivalStats": {
    "서울장미축제": 5,
    "K-핸드메이드페어": 3
  },
  "errorStats": {
    "차단됨": 1,
    "타임아웃": 1
  },
  "recentReviews": [
    {
      "id": 1,
      "festivalName": "서울장미축제",
      "title": "서울장미축제 후기",
      "isSuccess": true,
      "crawledAt": "2025-08-25 10:06:34"
    }
  ]
}
```

---

## 🤖 리뷰 분석 API

### 1. 개별 리뷰 분석
```http
POST /api/analysis/review/{reviewId}
```
**설명**: 특정 리뷰를 AI로 분석하여 감정, 키워드, 요약 생성

**응답**:
```json
{
  "id": 1,
  "festivalScore": 85,
  "positivePercentage": 80,
  "negativePercentage": 20,
  "positiveKeywords": "아름다움,축제,장미,환상적,추천",
  "negativeKeywords": "혼잡,비싼음식",
  "aiSummary": "서울장미축제에 대한 매우 긍정적인 후기입니다. 장미의 아름다움과 축제 분위기에 대한 만족도가 높으며, 특히 장미터널과 사진 촬영 구역이 인상적이었다는 평가가 많습니다.",
  "isSuccess": true,
  "analyzedAt": "2025-08-25 10:06:34"
}
```

### 2. 축제 전체 리뷰 분석
```http
POST /api/analysis/festival/{festivalName}
```
**설명**: 특정 축제의 모든 리뷰를 일괄 분석

### 3. 분석 결과 목록 조회
```http
GET /api/analysis/results
```
**설명**: 모든 리뷰 분석 결과 조회

**응답**:
```json
{
  "success": true,
  "totalCount": 10,
  "results": [
    {
      "id": 1,
      "festivalScore": 85,
      "positivePercentage": 80,
      "negativePercentage": 20,
      "positiveKeywords": "아름다움,축제,장미,환상적,추천",
      "negativeKeywords": "혼잡,비싼음식",
      "aiSummary": "서울장미축제에 대한 매우 긍정적인 후기입니다. 장미의 아름다움과 축제 분위기에 대한 만족도가 높으며, 특히 장미터널과 사진 촬영 구역이 인상적이었다는 평가가 많습니다.",
      "isSuccess": true,
      "analyzedAt": "2025-08-25 10:06:34"
    }
  ]
}
```

**참고**: 성공한 분석 결과만 반환됩니다.

### 4. 개별 분석 결과 삭제
```http
DELETE /api/analysis/{analysisId}
```
**설명**: 특정 분석 결과 삭제

**응답**:
```json
{
  "success": true,
  "message": "분석 데이터가 성공적으로 삭제되었습니다.",
  "deletedAnalysisId": 1
}
```

### 5. 분석 결과 일괄 삭제
```http
DELETE /api/analysis/batch
Content-Type: application/json

{
  "analysisIds": [1, 2, 3]
}
```
**설명**: 여러 분석 결과를 한 번에 삭제

**응답**:
```json
{
  "success": true,
  "message": "삭제 완료: 3개 분석 데이터",
  "deletedCount": 3,
  "notFoundIds": []
}
```

### 6. 모든 분석 데이터 삭제
```http
DELETE /api/analysis/clear
```
**설명**: 모든 분석 데이터 삭제

**응답**:
```json
{
  "success": true,
  "message": "모든 분석 데이터가 삭제되었습니다.",
  "deletedCount": 10
}
```

---

## 💬 챗봇 API

### 1. 챗봇 질문
```http
POST /api/chatbot/ask
Content-Type: application/json

{
  "question": "서울에서 6월에 열리는 축제가 있나요?"
}
```
**설명**: AI 챗봇에게 질문하고 관련 축제 정보와 함께 답변 받기

**응답**:
```json
{
  "answer": "찾으시는 행사는 5개 있어요. 주요 행사로는 'K-핸드메이드페어 2025', '2025 카즈미 타테이시 트리오 내한공연' 등이 있어요.",
      "relatedEvents": [
      {
        "id": 1,
        "name": "K-핸드메이드페어 2025",
        "district": "강남구",
        "date": "2025-12-18~2025-12-21"
      }
    ]
}
```

---



## 🔧 CORS 설정

현재 서버는 다음 도메인에서의 접근을 허용합니다:
- `http://localhost:3000` (React 개발 서버)
- `http://localhost:5173` (Vite 개발 서버)
- `http://127.0.0.1:3000`
- `http://127.0.0.1:5173`
- `http://3.38.210.80` (EC2 서버)
- `http://3.38.210.80:3000`
- `http://3.38.210.80:5173`
- `*` (모든 도메인 - 개발 중에만)

---

## 📊 에러 응답 형식

모든 API에서 에러 발생 시 다음과 같은 형식으로 응답합니다:

```json
{
  "success": false,
  "message": "에러 메시지",
  "timestamp": "2025-08-25T10:06:34"
}
```

---

## 🚀 사용 예시

### JavaScript (Fetch API)
```javascript
// 축제 데이터 수집
const response = await fetch('http://3.38.210.80:8081/api/festivals/fetch', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  }
});

// 챗봇 질문
const chatbotResponse = await fetch('http://3.38.210.80:8081/api/chatbot/ask', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    question: "서울에서 6월에 열리는 축제가 있나요?"
  })
});

const result = await chatbotResponse.json();
console.log(result.answer);
console.log(result.relatedEvents);
```

### Axios
```javascript
import axios from 'axios';

const API_BASE_URL = 'http://3.38.210.80:8081/api';

// 축제 목록 조회
const festivals = await axios.get(`${API_BASE_URL}/festivals`);

// 리뷰 크롤링
const crawlResult = await axios.post(`${API_BASE_URL}/crawler/reviews`, {
  festivalName: '서울장미축제',
  maxReviews: 5
});
```

---

## 📝 참고사항

1. **서버 상태**: 현재 EC2 서버에서 8081 포트로 실행 중
2. **데이터베이스**: MySQL 사용 (hackathon_db)
3. **AI 모델**: GPT-3.5-turbo 사용
4. **크롤링**: 네이버 블로그 리뷰 수집
5. **실시간 처리**: 모든 API는 실시간으로 처리됩니다

---

**문의사항**: 백엔드 개발팀에게 문의하세요! 🚀
