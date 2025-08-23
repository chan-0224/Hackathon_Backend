# API 명세서 (Frontend용)
---
## 📖 목차
1. [기본 정보](#📌-기본-정보)
2. [게시판](#📚​-게시판)
3. [좋아요](#👍​좋아요)
4. [댓글](#✏️​​댓글)
5. [이미지 업로드](#📷​​이미지-업로드)
6. [캘린더](#🗓️-캘린더)
7. [FAQ](#❓​-faq)
8. [공지사항](#📋​-공지사항)
9. [마이페이지](#👤​​-마이페이지)
10. [지역 설정](#📍지역-설정)
---
## 📌 기본 정보
- **Base URL:** `http://localhost:3001`(로컬 서버 주소)
- **Content-Type:** `application/json`(이미지 업로드 시 `multipart/form-data`)
- **인증:** 없음 (로그인 없이 사용 가능)

---

## 📚​ 게시판

**1. 게시글 작성**

- **URL:** `/posts`
- **Method:** `POST`
- **설명:** 새 글 작성 (이미지 첨부 가능)
- **Request Body (multipart/form-data):**
```json
{
  "author_name": "익명",
  "title": "글 제목",
  "content": "글 내용",
  "category": "free", 
  "image": "(파일, multipart/form-data)"
}
```
- **category 옵션**: `free`, `promotion_personal`, `promotion_official`, `news`, `hot`

- **promotion_official**: 관리자/AI만 작성 가능



- **Response:**
```
{
  "id": 1,
  "author_name": "익명",
  "title": "글 제목",
  "content": "글 내용",
  "category": "free",
  "image_url": "/uploads/123456.jpg",
  "likes": 0,
  "views": 0,
  "created_at": "2025-08-21T03:00:00.000Z",
  "updated_at": "2025-08-21T03:00:00.000Z"
}
```
**Status Codes**

- `201 Created` 성공

- `400 Bad Request` 필수값 누락, HOT 게시판 작성 시도

- `403 Forbidden` 공식 홍보 게시판 권한 없음

- `500 Internal Server Error` 서버 오류

**2. 게시글 목록 조회**

- **URL:**: `/posts?category=free&page=1&limit=10&search=키워드&sort=asc`

- **Method**: GET

- **Query Parameters**:

- `category`: `free` | `promotion_personal` | `promotion_official` | `news` | `hot`
- `page`: 1 (default)
- `limit`: 10 (default)
- `search`: 검색어 (optional)
- `sort`: `asc` | `desc` (optional)

- **설명**: hot게시판 제외 페이지네이션, 검색, 정렬 가능

- **Response**:
```
  {
    "id": 1,
    "author_name": "익명",
    "title": "글 제목",
    "content": "글 내용",
    "category": "free",
    "image_url": "/uploads/123456.jpg",
    "likes": 5,
    "views": 12,
    "created_at": "2025-08-21T03:00:00.000Z",
    "updated_at": "2025-08-21T03:00:00.000Z"
  }
```
**Status Codes**

- `200 OK` 성공
- `500 Internal Server Error` 서버 오류


**🔥​HOT 게시판(category=hot)**
- **조회 방식**: `GET` /`posts?category=hot&page=1&limit=10`

- **정렬 기준**: 좋아요 수(likes), 조회수(views) 기준 내림차순

- **주의**: - HOT 게시판 글은 직접 작성 불가(프론트에서 버튼 비활성화 필요), 좋아요 수가 10 이상이면 HOT 게시판으로 이동, 페이지네이션 가능

**3. 특정 게시글 조회**


- **URL**:`/posts/:id`
  → 조회수 자동 +1

- **Method**: `GET`

- **Response**:
```
{
  "id": 1,
  "author_name": "익명",
  "title": "글 제목",
  "content": "글 내용",
  "views": 13,
  "likes": 5,
  "created_at": "...",
  "updated_at": "..."
}
```
- `200 OK` 성공
- `404 Not Found` 없음

**4. 게시글 수정**

- **URL**:`/posts/:id`

- **Method**: `PUT`

- **설명**: 글 수정 (이미지 포함 가능)


- **Request Body**:
```
{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "category": "free",
  "image": "(파일, multipart/form-data)"
}
```
- **⚠️주의**: HOT 게시판 글은 수정 불가
- 공식 홍보 게시판 수정은 관리자/AI만 가능

- **Response**:
```
{
  "id": 1,
  "author_name": "익명",
  "title": "수정된 제목",
  "content": "수정된 내용",
  "category": "free",
  "image_url": "/uploads/789012.jpg",
  "created_at": "2025-08-21T03:00:00.000Z",
  "updated_at": "2025-08-21T03:30:00.000Z"
}
```
**5. 게시글 삭제**

- **URL**:`/posts/:id`

- **Method**: `DELETE`

- **설명**: 글 삭제


- **Response**:
```
{
  "message": "글이 삭제되었습니다."
}
```

**6. AI 자동 등록**

- **URL**:`/posts/ai/create`

- **Method**: `POST`

- **Request**:
```
{
  "title": "AI가 수집한 뉴스",
  "content": "내용",
  "category": "news"
}
```
-category: `promotion_official` | `news` 만 허용

- **Response**:
```
{
   "id": 99,
  "author_name": "AI",
  "title": "AI가 수집한 뉴스",
  "content": "내용",
  "category": "news"
}
```
- `200 Created` 성공
- `404 Bad Request` 필수값 누락/잘못된 카테고리


**👍​좋아요**

**1. 좋아요 추가**

- **URL**:`/likes`

- **Method**: `POST`

- **Request Body**:
```
{
 "post_id": 1, "author_name": "익명" 
}
```

- **Response**:
```
{
  "id": 1, "post_id": 1, "author_name": "익명", "created_at": "..." 
}
```
- *이미 좋아요 누른 경우 → `{ "message": "이미 좋아요함" }`

**Status Codes**
- `200 OK` 성공

- `400 Bad Request` 필수값 누락

- `500 Internal Server Error` 서버 오류

**2. 좋아요 삭제**

- **URL**:`/likes`

- **Method**: `DELETE`

- **Request Body**:
```
{
"post_id": 1, "author_name": "익명"
}
```

- **Response**:
```
{
  "message": "좋아요 취소됨"
}
```
- `200 OK` 성공

- `400 Bad Request` 필수값 누락

- `404 Not Found` 좋아요 기록 없음

- `500 Internal Server Error` 서버 오류

**3. 특정 글 좋아요 수 조회**

- **URL**:`/likes?post_id=1`

- **Method**: `GET`

- **Response**:
```
{
  "likes": 5
}
```

- `200 OK` 성공

- `400 Bad Request` post_id 누락

**4. 특정 사용자의 좋아요 여부 확인**

- **URL**:`/likes/check?post_id=1&author_name=익명`

- **Method**: `GET`

- **Response**:
```
{
  "liked": true
}
```

- `200 OK` 성공

- `400 Bad Request` 필수값 누락


**✏️​​댓글**

**1. 댓글 작성**

- **URL**:`/comments`

- **Method**: `POST`

- **Request Body**:
```
{
"post_id": 1,
  "author_name": "익명",
  "content": "댓글 내용"
}
```

- **Response**:
```
{
   "id": 1,
  "post_id": 1,
  "author_name": "익명",
  "content": "댓글 내용",
  "created_at": "...",
  "updated_at": "..."
}
```

**Status Codes**
- `200 OK` 성공

- `400 Bad Request` post_id/content 누락

**2. 댓글 목록 조회**

- **URL**:`/comments?post_id=1`

- **Method**: `GET`

- **Response**:
```
{
  "id": 1,
    "post_id": 1,
    "author_name": "익명",
    "content": "댓글 내용",
    "created_at": "...",
    "updated_at": "..."
}
```
- `200 OK` 성공

- `400 Bad Request` post_id 누락

**3. 댓글 수정**

- **URL**:`/comments/:id`

- **Method**: `PUT`

- **Request Body**:
```
{
  "content": "수정된 댓글 내용"
}
```
- **Response**:
```
{
  "id": 1,
  "post_id": 1,
  "author_name": "익명",
  "content": "수정된 댓글 내용",
  "updated_at": "..."
}
```
- `200 OK` 성공

- `400 Bad Request` content 누락

- `404 Not Found` 댓글 없음

**4. 댓글 삭제**

- **URL**:`/comments/:id`

- **Method**: `DELETE`

- **Response**:
```
{
  "message": "댓글 삭제됨"
}
```

- `200 OK` 성공

- `404 Not Found` 댓글 없음

## 📷​​이미지 업로드

- **업로드 방식**: `multipart/form-data`

- **Field Name**: `image`

- **Response**: `/uploads/파일명`

## ✅ 참고 사항
- 모든 API는 로그인 없이 사용 가능

- 조회수, 좋아요 정보는 자동 업데이트

- 게시판 기능은 free, promotion-personal, promotion-official, news, hot 총 5개 카테고리로 분류

---

## 🗓️ 캘린더

**1. 이벤트 생성**

- **URL:** `/events`
- **Method:** `POST`
- **설명:** 새로운 이벤트 생성
- **Request Body:**
```json
{
  "title": "부산 불꽃축제",  
  "description": "광안리 해수욕장에서 열리는 불꽃축제", 
  "start_date": "2025-10-05",
  "end_date": "2025-10-05",   
  "location": "부산 광안리 해수욕장", 
  "is_public": true              
}
```

- **Response:**
```
{
   "id": 1,
  "title": "부산 불꽃축제",
  "description": "광안리 해수욕장에서 열리는 불꽃축제",
  "start_date": "2025-10-05",
  "end_date": "2025-10-05",
  "location": "부산 광안리 해수욕장",
  "is_public": true,
  "created_at": "2025-08-23T03:00:00.000Z",
  "updated_at": "2025-08-23T03:00:00.000Z"
}
```
**Status Codes:**

- `201 Created` 성공
- `400 Bad Request`: 필수값 누락
- `500 Internal Server Error` 서버 오류


**2. 이벤트 조회**

- **URL:**: `/events`

- **Method**: `GET`

- **설명**: 전체 이벤트, 공개 이벤트, 다가오는/진행중 일정 조회 가능

- **Query Parameters**:

- public_only | boolean | true → 공개 이벤트만 조회
- upcoming_or_ongoing | boolean | true → 오늘 기준 진행중/다가오는 일정만 조회


- **Response**:
```
  {
     "id": 1,
    "title": "부산 불꽃축제",
    "description": "광안리 해수욕장에서 열리는 불꽃축제",
    "start_date": "2025-10-05",
    "end_date": "2025-10-05",
    "location": "부산 광안리 해수욕장",
    "is_public": true,
    "created_at": "2025-08-23T03:00:00.000Z",
    "updated_at": "2025-08-23T03:00:00.000Z"
  }
```
**Status Codes**

- `200 OK` 성공
- `500 Internal Server Error` 서버 오류


**3. 특정 이벤트 조회**

- **URL**:`/events/:id`

- **Method**: `GET`

- **설명**: 특정 이벤트 조회

- **Response**:
```
{
  "id": 1,
  "title": "부산 불꽃축제",
  "description": "광안리 해수욕장에서 열리는 불꽃축제",
  "start_date": "2025-10-05",
  "end_date": "2025-10-05",
  "location": "부산 광안리 해수욕장",
  "is_public": true,
  "created_at": "2025-08-23T03:00:00.000Z",
  "updated_at": "2025-08-23T03:00:00.000Z"
}
```
**Status Codes:**
- `200 OK` 성공
- `404 Not Found` 이벤트 없음

**4. 이벤트 수정**

- **URL**:`/events/:id`

- **Method**: `PUT`

- **설명**: 이벤트 수정


- **Request Body**:
```
{
  "title": "서울 불꽃축제",
  "description": "한강에서 열리는 불꽃축제",
  "start_date": "2025-10-10",
  "end_date": "2025-10-10",
  "location": "서울 여의도 한강공원",
  "is_public": true
}
```

- **Response**:
```
{
   "id": 1,
  "title": "서울 불꽃축제",
  "description": "한강에서 열리는 불꽃축제",
  "start_date": "2025-10-10",
  "end_date": "2025-10-10",
  "location": "서울 여의도 한강공원",
  "is_public": true,
  "created_at": "2025-08-23T03:00:00.000Z",
  "updated_at": "2025-08-23T03:05:00.000Z"
}
```
**Status Codes:**
- `200 OK`: 성공

- `404 Not Found`: 이벤트 없음

- `500 Internal Server Error`: 서버 오류

**5. 이벤트 삭제**

- **URL**:`/events/:id`

- **Method**: `DELETE`

- **설명**: 이벤트 삭제

- **Response**:
```
{
  "message": "삭제됨"
}
```
**Status Codes:**
- `200 OK`: 성공

- `404 Not Found`: 이벤트 없음

- `500 Internal Server Error`: 서버 오류


**​⚠️​주의사항**

**1. 좋아요/관심 이벤트 표시**

- 로그인 기능 없음 → localStorage 사용

- 관심 이벤트 ID 배열에 저장 후 캘린더에 표시

**2. 진행중/다가오는 일정**

`upcoming_or_ongoing=true` 옵션 사용

오늘 기준 start_date ≤ 오늘 ≤ end_date 이벤트 강조

**3. 세부 정보 표시**

이벤트 클릭 → `title, description, start_date, end_date, location` 모달 표시

---

## ❓​ FAQ

**1. 전체 FAQ 조회**

- **URL:** `/faq`
- **Method:** `GET`
- **설명:** 전체 FAQ 목록 조회

- **Response:**
```
{
  "id": 1,
    "question": "회원가입은 어떻게 하나요?",
    "answer": "홈페이지 상단의 가입 버튼을 클릭 후 정보를 입력하면 됩니다.",
    "created_at": "2025-08-23T03:00:00.000Z",
    "updated_at": "2025-08-23T03:00:00.000Z"
}
```

**Status Codes:**

- `200 OK` 성공
- `500 Internal Server Error` 서버 오류


**2. FAQ 추가 (관리자용)**

- **URL:**: `/faq`

- **Method**: `POST`

- **설명**: 새로운 FAQ 추가. 관리자 권한 필요.

- **Request Body**:
```
{
 "question": "질문 내용",
  "answer": "답변 내용"
}
```

- **Response**:
```
  {
   "id": 2,
  "question": "질문 내용",
  "answer": "답변 내용",
  "created_at": "2025-08-23T03:05:00.000Z",
  "updated_at": "2025-08-23T03:05:00.000Z"
  }
```
**Status Codes**

- `201 Created` 성공
- `400 Bad Request` 질문/답변 누락
- `500 Internal Server Error` 서버 오류

---

## 📋​ 공지사항

**1. 전체 공지사항 조회**

- **URL:** `/notice`
- **Method:** `GET`
- **설명:** 전체 공지사항 목록 조회

- **Response:**
```
{
  "id": 1,
    "title": "서비스 점검 안내",
    "content": "8월 24일 00:00~06:00 서비스 점검이 진행됩니다.",
    "created_at": "2025-08-23T03:00:00.000Z",
    "updated_at": "2025-08-23T03:00:00.000Z"
}
```

**Status Codes:**

- `200 OK` 성공
- `500 Internal Server Error` 서버 오류


**2. 공지사항 추가**

- **URL:**: `/notice`

- **Method**: `POST`

- **설명**: 새로운 FAQ 추가. 관리자 권한 필요.

- **Request Body**:
```
{
  "title": "공지 제목",
  "content": "공지 내용"
}
```

- **Response**:
```
  {
   "id": 2,
  "title": "공지 제목",
  "content": "공지 내용",
  "created_at": "2025-08-23T03:05:00.000Z",
  "updated_at": "2025-08-23T03:05:00.000Z"
  }
```
**Status Codes**

- `201 Created` 성공
- `400 Bad Request` 제목/내용 누락
- `500 Internal Server Error` 서버 오류

---

## 👤​​ 마이페이지

**1. 마이페이지 조회**

- **URL:** `/mypage`
- **Method:** `GET`
- **설명:** 마이페이지에 표시될 모든 정보 조회

- 게시글, 댓글, 받은 좋아요 수, 관심 이벤트, 프로필 정보 포함

- **Response 예시:**
```
{
  "profile": {
    "id": 1,
    "nickname": "사용자",
    "bio": "설명",
    "avatar_url": "사용자 프로필 사진 URL"
  },
  "posts": [
    {
      "id": 1,
      "title": "첫 글",
      "content": "테스트용 첫 글입니다",
      "created_at": "2025-08-23T11:00:00.000Z"
    },
    {
      "id": 2,
      "title": "두 번째 글",
      "content": "두 번째 글 내용",
      "created_at": "2025-08-23T12:00:00.000Z"
    }
  ],
  "comments": [
    {
      "id": 1,
      "post_id": 1,
      "content": "첫 댓글",
      "created_at": "2025-08-23T11:10:00.000Z"
    },
    {
      "id": 2,
      "post_id": 2,
      "content": "두 번째 댓글",
      "created_at": "2025-08-23T12:10:00.000Z"
    }
  ],
  "likes": 3,
  "interested_events": [
    {
      "id": 1,
      "event_name": "축제 A",
      "event_date": "2025-08-25"
    },
    {
      "id": 2,
      "event_name": "행사 B",
      "event_date": "2025-08-30"
    }
  ]
}
```

**Status Codes:**

- `200 OK` 성공
- `404 Not Found` – 프로필 데이터 없음
- `500 Internal Server Error` 서버 오류


**2. 프로필 수정**

- **URL:**: `/mypage/profile`

- **Method**: `PUT`

- **설명**: 프로필 정보 수정 가능 (닉네임, 소개, 아바타)

- **Request Body**:
```
{
  "nickname": "사용자",
  "bio": "테스트",
  "avatar_url": "사용자 프로필 사진 URL"
}
```

- **Response**:
```
  {
  "id": 1,
  "nickname": "사용자",
  "bio": "테스트",
  "avatar_url": "사용자 프로필 사진 URL"
  }
```
**Status Codes**

- `201 Created` 성공
- `400 Bad Request` 필수값 누락
- `500 Internal Server Error` 서버 오류

---

## 📍지역 설정

**1. 지역 목록 조회**

- **URL:** `/regions/list`
- **Method:** `GET`
- **설명:** DB에 저장된 지역 목록 조회 (서울 25개 구 단위 기준)

- **Response 예시:**
```
{
    "regions":[
    { "id": 1, "district": "강남구", "city": "서울" },
    { "id": 2, "district": "서초구", "city": "서울" },
    { "id": 3, "district": "송파구", "city": "서울" }
    ]
}
```

**Status Codes:**

- `200 OK` 정상 조회
- `500 Internal Server Error` 서버 오류


**2. 선택한 지역 기반 AI 행사 조회**

- **URL:**: `/regions/events`

- **Method**: `GET`

- **설명**: 선택한 지역(district)을 기준으로 AI 백엔드에서 행사 정보 조회

- **Query Parameter:**:
  - `district` (string, 필수) – ex: `'강남구'`

- **Response 예시**:
```
  {
  "events": [
    {
      "id": 1,
      "title": "강남 문화 축제",
      "description": "강남에서 열리는 문화 행사",
      "start_date": "2025-09-01",
      "end_date": "2025-09-03",
      "location": "강남구청",
      "is_public": true
    },
    {
      "id": 2,
      "title": "강남 음악회",
      "description": "지역 음악회",
      "start_date": "2025-09-10",
      "end_date": "2025-09-10",
      "location": "코엑스",
      "is_public": true
    }
  ]
}
```
**Status Codes**

- `201 Created` 정상 조회
- `400 Bad Request` `district` 쿼리 누락
- `500 Internal Server Error` AI 백엔드 호출 실패
gi