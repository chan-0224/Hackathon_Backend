# 서울시 문화행사 게시판 & 추천 시스템

Spring Boot 기반의 서울시 문화행사 정보를 활용한 게시판 및 추천 시스템입니다.

## 🚀 주요 기능

- **문화행사 데이터 수집**: 서울시 문화행사 API 연동으로 실시간 데이터 수집
- **게시판 시스템**: 구별 문화행사 게시판 기능
- **검색 및 필터링**: 지역별, 이름별 문화행사 검색
- **자동 데이터 동기화**: 스케줄링을 통한 정기적 데이터 업데이트

## 🛠️ 기술 스택

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **Spring WebFlux** (API 호출용)
- **MySQL 8.0**
- **Lombok**

## 📋 프로젝트 구조

```
src/main/java/com/example/hackathon/
├── entity/          # JPA 엔티티
│   ├── User.java
│   ├── Post.java
│   └── Festival.java
├── repository/      # JPA 리포지토리
│   ├── UserRepository.java
│   ├── PostRepository.java
│   └── FestivalRepository.java
├── service/         # 비즈니스 로직
│   └── FestivalService.java
├── controller/      # REST API 컨트롤러
│   └── FestivalController.java
├── dto/            # 데이터 전송 객체
│   ├── CulturalEventApiResponse.java
│   ├── CulturalEventInfo.java
│   └── CulturalEventRow.java
└── config/         # 설정 클래스
    └── WebClientConfig.java
```

## ⚙️ 설치 및 실행

### 1. 사전 요구사항
- Java 17 이상
- MySQL 8.0
- Gradle

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
DB_URL=jdbc:mysql://localhost:3306/hackathon_db?serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# API 키 설정
SEOUL_CULTURE_API_KEY=your_seoul_api_key
CHATGPT_API_KEY=your_chatgpt_api_key
```

### 5. 애플리케이션 실행
```bash
./gradlew bootRun
```

애플리케이션이 `http://localhost:8081`에서 실행됩니다.

## 📊 데이터베이스 스키마

### User (사용자)
- `id`: 기본키 (자동생성)
- `username`: 사용자명
- `password`: 비밀번호
- `name`: 이름
- `email`: 이메일 (고유)
- `phone`: 전화번호

### Post (게시글)
- `id`: 기본키 (자동생성)
- `title`: 제목
- `content`: 내용
- `author`: 작성자 (User와 다대일 관계)
- `district`: 지역구
- `createdAt`: 작성시간 (자동생성)

### Festival (문화행사)
- `id`: 기본키 (자동생성)
- `name`: 행사명
- `date`: 행사일정
- `district`: 개최지역
- `place`: 장소
- `link`: 상세정보 링크
- `uniqueKey`: 중복방지용 고유키

## 🌐 API 엔드포인트

### 문화행사 API
- `POST /api/festivals/fetch` - 수동 데이터 수집
- `GET /api/festivals` - 전체 문화행사 조회
- `GET /api/festivals/district/{district}` - 구별 문화행사 조회
- `GET /api/festivals/search?name={name}` - 이름으로 검색
- `GET /api/festivals/search/advanced?district={district}&name={name}` - 고급 검색
- `GET /api/festivals/status` - 서버 상태 확인

### 테스트 페이지
- `GET /test.html` - API 테스트용 웹 페이지

## 🔄 데이터 수집

### 수동 데이터 수집
```bash
curl -X POST http://localhost:8081/api/festivals/fetch
```

### 자동 데이터 수집
매일 새벽 2시에 자동으로 데이터가 수집됩니다. (스케줄링 기능)

## 🔧 설정

### 주요 설정값
- **서버 포트**: 8081
- **데이터베이스**: MySQL (hackathon_db)
- **JPA**: Hibernate with MySQL8Dialect
- **로깅**: 개발환경에서 SQL 쿼리 로그 출력

### 환경변수
- `DB_URL`: 데이터베이스 연결 URL
- `DB_USERNAME`: 데이터베이스 사용자명
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `SEOUL_CULTURE_API_KEY`: 서울시 문화행사 API 키
- `CHATGPT_API_KEY`: ChatGPT API 키

## 🚨 주의사항

1. **API 키 보안**: 실제 API 키는 환경변수나 별도 설정 파일에서 관리하세요
2. **데이터베이스 접속정보**: 실제 운영환경에서는 강력한 비밀번호를 사용하세요
3. **포트 설정**: 8081 포트가 사용 중인 경우 `application.properties`에서 변경 가능합니다

## 📝 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.