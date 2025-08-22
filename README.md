# 🎭 서울시 문화행사 AI 챗봇 & 추천 시스템

> **Spring Boot 기반의 서울시 문화행사 정보를 활용한 AI 챗봇 및 추천 시스템**

## 📋 프로젝트 개요

이 프로젝트는 서울시 문화행사 정보를 활용하여 다음과 같은 기능을 제공합니다:

- **🤖 AI 챗봇**: 자연어로 문화행사 정보를 질문하고 답변받을 수 있는 챗봇
- **🎯 태그 기반 추천**: AI가 생성한 태그를 기반으로 맞춤형 문화행사 추천
- **📊 데이터 수집**: 서울시 문화행사 API 및 네이버 블로그 크롤링을 통한 데이터 수집
- **🧠 AI 분석**: OpenAI GPT를 활용한 리뷰 감정 분석 및 축제 요약 생성

## 🚀 주요 기능

### 🤖 AI 챗봇 시스템
- **자연어 질문 처리**: "이번 주말 강남구에서 하는 문화 공연 알려줘" 같은 자연어 질문 처리
- **키워드 추출**: GPT를 통한 지역, 날짜, 행사 종류, 행사명 자동 추출
- **지능형 검색**: 추출된 키워드를 기반으로 관련 문화행사 검색
- **자연어 답변**: 검색 결과를 바탕으로 친근하고 자연스러운 답변 생성

### 🎯 태그 기반 추천 시스템
- **AI 태그 생성**: 축제 정보를 바탕으로 분위기 및 활동 태그 자동 생성
- **맞춤형 추천**: 사용자 선호 태그를 기반으로 관련 문화행사 추천
- **페이징 지원**: 대용량 데이터 처리를 위한 페이징 기능

### 📊 데이터 수집 및 관리
- **서울시 API 연동**: 서울시 문화행사 API를 통한 실시간 데이터 수집
- **네이버 블로그 크롤링**: 축제 관련 리뷰 및 후기 수집
- **AI 리뷰 분석**: 수집된 리뷰의 감정 분석 및 점수화
- **자동 동기화**: 스케줄링을 통한 정기적 데이터 업데이트

## 🛠️ 기술 스택

### Backend
- **Java 17** - 메인 프로그래밍 언어
- **Spring Boot 3.2.0** - 웹 애플리케이션 프레임워크
- **Spring Data JPA** - 데이터베이스 ORM
- **Spring WebFlux** - 비동기 HTTP 클라이언트
- **Spring Retry** - 재시도 로직

### Database
- **MySQL 8.0** - 메인 데이터베이스
- **Hibernate** - JPA 구현체

### AI & External APIs
- **OpenAI GPT-3.5-turbo** - 자연어 처리 및 AI 분석
- **서울시 문화행사 API** - 문화행사 데이터 수집
- **Jsoup** - 웹 크롤링

### Development Tools
- **Gradle** - 빌드 도구
- **Lombok** - 보일러플레이트 코드 제거
- **Jackson** - JSON 처리

## 📁 프로젝트 구조

```
src/main/java/com/example/hackathon/
├── config/          # 설정 클래스
│   ├── OpenAIConfig.java      # OpenAI API 설정
│   ├── WebClientConfig.java   # HTTP 클라이언트 설정
│   └── CrawlerConfig.java     # 크롤링 설정
├── controller/      # REST API 컨트롤러
│   ├── ChatbotController.java     # 챗봇 API
│   ├── EventsController.java      # 문화행사 API
│   ├── CrawlerController.java     # 크롤링 API
│   └── ReviewAnalysisController.java # 리뷰 분석 API
├── service/         # 비즈니스 로직
│   ├── ChatbotService.java        # 챗봇 서비스
│   ├── FestivalService.java       # 문화행사 서비스
│   ├── FestivalAIService.java     # AI 분석 서비스
│   ├── NaverBlogCrawlerService.java # 크롤링 서비스
│   └── ReviewAnalysisService.java # 리뷰 분석 서비스
├── entity/          # JPA 엔티티
│   ├── Festival.java         # 문화행사 정보
│   ├── Review.java           # 크롤링된 리뷰
│   ├── ReviewAnalysis.java   # 리뷰 분석 결과
│   ├── User.java             # 사용자 정보
│   └── Post.java             # 게시글 정보
├── repository/      # JPA 리포지토리
│   ├── FestivalRepository.java
│   ├── ReviewRepository.java
│   ├── ReviewAnalysisRepository.java
│   ├── UserRepository.java
│   └── PostRepository.java
├── dto/            # 데이터 전송 객체
│   ├── ChatbotRequest.java       # 챗봇 요청 DTO
│   ├── ChatbotResponse.java      # 챗봇 응답 DTO
│   ├── QuestionAnalysis.java     # 질문 분석 DTO
│   └── CulturalEvent*.java       # 문화행사 API DTO
└── HackathonApplication.java     # 메인 애플리케이션 클래스
```

## ⚙️ 설치 및 실행

### 1. 사전 요구사항
- **Java 17** 이상
- **MySQL 8.0**
- **Gradle 7.x** 이상

### 2. 저장소 클론
```bash
git clone <repository-url>
cd hackathon
```

### 3. 데이터베이스 설정
MySQL에서 데이터베이스를 생성합니다:
```sql
CREATE DATABASE hackathon_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4. 환경 설정
`src/main/resources/application-local.properties.example` 파일을 복사하여 `application-local.properties` 파일을 생성하고 실제 값으로 수정합니다:

```properties
# 데이터베이스 설정
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

# API 키 설정
seoul.culture.api.key=your_seoul_api_key
openai.api.key=your_openai_api_key
```

### 5. 애플리케이션 실행
```bash
./gradlew bootRun
```

애플리케이션이 `http://localhost:8081`에서 실행됩니다.

## 📊 데이터베이스 스키마

### Festival (문화행사)
- `id`: 기본키 (자동생성)
- `name`: 행사명
- `date`: 행사 기간
- `district`: 행사 지역 (구 단위)
- `place`: 행사 장소
- `link`: 행사 관련 링크
- `uniqueKey`: 중복 방지용 고유 키
- `aiSummary`: AI가 생성한 요약 정보 (JSON)
- `moodTags`: AI가 생성한 분위기 태그 (JSON 배열)
- `createdAt`: 생성일시
- `updatedAt`: 수정일시

### Review (리뷰)
- `id`: 기본키 (자동생성)
- `festivalName`: 축제 이름 (검색 키워드)
- `title`: 블로그 제목
- `blogUrl`: 블로그 URL
- `author`: 블로그 작성자
- `content`: 리뷰 본문 내용
- `postDate`: 리뷰 작성일
- `isSuccess`: 크롤링 성공 여부
- `errorMessage`: 오류 메시지
- `userAgent`: 크롤링 시 사용한 User-Agent
- `crawledAt`: 크롤링 요청 시간
- `createdAt`: 생성일시
- `updatedAt`: 수정일시

### ReviewAnalysis (리뷰 분석)
- `id`: 기본키 (자동생성)
- `review`: 분석 대상 리뷰 (Review와 1:N 관계)
- `festivalScore`: 축제 평가 점수 (0-100점)
- `analysisResult`: 전체 분석 결과 (JSON)
- `isSuccess`: 분석 성공 여부
- `errorMessage`: 오류 메시지
- `analyzedAt`: 분석 완료 시간
- `createdAt`: 생성일시
- `updatedAt`: 수정일시

## 🔒 보안 설정

### ⚠️ 중요 보안 주의사항

이 프로젝트는 외부 API 키와 데이터베이스 접속 정보를 사용합니다. 다음 보안 가이드라인을 반드시 준수해주세요:

#### **1. API 키 보안**
- **절대 GitHub에 API 키를 커밋하지 마세요!**
- OpenAI API 키와 서울시 API 키는 환경변수나 `application-local.properties`에서 관리
- `application-local.properties` 파일은 `.gitignore`에 포함되어 GitHub에 업로드되지 않음

#### **2. 데이터베이스 보안**
- 데이터베이스 비밀번호는 환경변수로 관리
- 기본 비밀번호(`0000`)는 개발용으로만 사용하고, 운영환경에서는 강력한 비밀번호 사용
- 데이터베이스 접속 정보는 절대 소스코드에 하드코딩하지 마세요

#### **3. 환경변수 설정**
```bash
# Windows PowerShell
$env:OPENAI_API_KEY="your_openai_api_key_here"
$env:SEOUL_CULTURE_API_KEY="your_seoul_api_key_here"
$env:DB_USERNAME="your_db_username"
$env:DB_PASSWORD="your_db_password"

# Windows Command Prompt
set OPENAI_API_KEY=your_openai_api_key_here
set SEOUL_CULTURE_API_KEY=your_seoul_api_key_here
set DB_USERNAME=your_db_username
set DB_PASSWORD=your_db_password

# Linux/Mac
export OPENAI_API_KEY="your_openai_api_key_here"
export SEOUL_CULTURE_API_KEY="your_seoul_api_key_here"
export DB_USERNAME="your_db_username"
export DB_PASSWORD="your_db_password"
```

#### **4. 로컬 설정 파일**
1. `src/main/resources/application-local.properties.example` 파일을 복사
2. `application-local.properties`로 이름 변경
3. 실제 API 키와 비밀번호로 수정
4. 이 파일은 자동으로 `.gitignore`에 포함됨

#### **5. API 키 발급 방법**
- **OpenAI API 키**: https://platform.openai.com/api-keys
- **서울시 OpenAPI 키**: https://data.seoul.go.kr/

## 🚨 주의사항

### **크롤링 관련**
- 네이버 서비스 이용약관을 준수하세요
- 과도한 요청으로 인한 IP 차단에 주의하세요
- 수집된 데이터의 저작권을 존중하세요
- 개발/테스트 목적으로만 사용하세요

### **API 사용량**
- OpenAI API는 사용량에 따라 요금이 부과됩니다
- 서울시 OpenAPI는 일일 요청 제한이 있습니다
- 프로덕션 환경에서는 적절한 캐싱과 제한을 설정하세요

### **데이터 보호**
- 개인정보가 포함된 데이터는 절대 수집하지 마세요
- 수집된 데이터는 적절한 보안 조치를 취하세요
- 데이터 백업 시에도 암호화를 고려하세요

## 📝 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

**⚠️ 기여 시 주의사항:**
- API 키나 비밀번호가 포함된 파일은 절대 커밋하지 마세요
- 새로운 의존성 추가 시 보안 취약점을 확인하세요
- 코드 리뷰 시 보안 관련 이슈를 중점적으로 검토하세요