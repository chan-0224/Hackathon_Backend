# 🚀 Java Spring Boot 백엔드

## 📋 개요
서울시 문화행사 API 연동 및 AI 분석을 담당하는 Spring Boot 백엔드 서비스입니다.

## 🎯 주요 기능
- **문화행사 데이터 수집**: 서울시 문화행사 API 연동
- **AI 분석**: OpenAI GPT를 활용한 축제 요약 및 태그 생성
- **크롤링**: 네이버 블로그 리뷰 수집 및 분석
- **추천 시스템**: 태그 기반 문화행사 추천
- **챗봇**: 자연어 기반 문화행사 검색

## 🛠️ 기술 스택
- **Java 17** + **Spring Boot 3.2**
- **Spring Data JPA** + **MySQL 8.0**
- **OpenAI GPT-3.5-turbo** + **WebClient**
- **Jsoup** (웹 크롤링)
- **Gradle** (빌드 도구)

## 🚀 실행 방법

### 1. 환경 설정
```bash
# 환경변수 설정
export OPENAI_API_KEY="your_openai_api_key"
export SEOUL_CULTURE_API_KEY="your_seoul_api_key"
export DB_USERNAME="your_db_username"
export DB_PASSWORD="your_db_password"
```

### 2. 데이터베이스 설정
```sql
CREATE DATABASE hackathon_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 애플리케이션 실행
```bash
cd backend/java-backend
./gradlew bootRun
```

## 📡 API 엔드포인트

### 문화행사 관리
- `POST /api/events/fetch` - 문화행사 데이터 수집
- `GET /api/events` - 전체 문화행사 목록
- `GET /api/events/{id}` - 특정 문화행사 상세
- `GET /api/events/district/{district}` - 구별 문화행사
- `GET /api/events/search` - 이름으로 검색
- `GET /api/events/recommend` - 태그 기반 추천

### 챗봇
- `POST /api/chatbot/ask` - 자연어 질문 처리

## 🔧 설정 파일
- `src/main/resources/application.properties` - 기본 설정
- `src/main/resources/application-local.properties` - 로컬 개발 설정 (gitignore)

## 📊 데이터베이스 스키마
- **Festival**: 문화행사 정보 + AI 생성 데이터
- **Review**: 크롤링된 리뷰
- **ReviewAnalysis**: AI 분석 결과

## ⚠️ 주의사항
- API 키는 환경변수로 관리
- 크롤링 시 네이버 서비스 이용약관 준수
- OpenAI API 사용량 모니터링 필요
