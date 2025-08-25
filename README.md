# 🎭 서울시 문화행사 AI 추천 플랫폼

## 📁 프로젝트 구조

```
hackathon/
├── 📱 frontend/                    # React 프론트엔드
│   ├── LA--/                      # React 앱 (Vite + Tailwind)
│   └── README.md                  # 프론트엔드 가이드
├── 🚀 backend/                     # 백엔드 서비스들
│   ├── java-backend/              # Java Spring Boot 백엔드
│   │   ├── src/                   # Java 소스코드
│   │   ├── build.gradle           # Gradle 빌드 설정
│   │   └── README.md              # Java 백엔드 가이드
│   └── node-backend/              # Node.js Express 백엔드
│       ├── src/                   # Node.js 소스코드
│       ├── package.json           # NPM 의존성
│       └── README.md              # Node.js 백엔드 가이드
├── 📚 docs/                        # 프로젝트 문서
│   ├── PROJECT_SUMMARY.md         # 프로젝트 전체 요약
│   └── API_DOCS.md                # API 문서
├── 🔒 security/                    # 보안 관련 도구
│   ├── security-check.sh          # Linux/Mac 보안 검사
│   └── security-check.bat         # Windows 보안 검사
├── 📋 README.md                    # 프로젝트 메인 가이드
└── .gitignore                      # Git 제외 파일 설정
```

## 🎯 주요 기능

### **프론트엔드 (React)**
- AI 기반 문화행사 추천
- 게시판 시스템
- 마이페이지
- 지역 기반 맞춤 서비스

### **Java 백엔드**
- 서울시 문화행사 API 연동
- OpenAI GPT 기반 AI 분석
- 네이버 블로그 크롤링
- 태그 기반 추천 시스템

### **Node.js 백엔드**
- 커뮤니티 게시판
- 댓글 및 좋아요 시스템
- 사용자 관리
- 이미지 업로드

## 🚀 빠른 시작

### **1. Java 백엔드 실행**
```bash
cd backend/java-backend
./gradlew bootRun
```

### **2. Node.js 백엔드 실행**
```bash
cd backend/node-backend
npm install
npm start
```

### **3. React 프론트엔드 실행**
```bash
cd frontend/LA--
npm install
npm run dev
```

## 🔧 환경 설정

### **필요한 환경변수**
```bash
# Java 백엔드
OPENAI_API_KEY=your_openai_api_key
SEOUL_CULTURE_API_KEY=your_seoul_api_key
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Node.js 백엔드
DB_USER=your_postgres_user
DB_PASSWORD=your_postgres_password
DB_HOST=localhost
DB_NAME=hackathon_db
DB_PORT=5432
```

## 📊 기술 스택

- **Frontend**: React 19 + Vite + Tailwind CSS
- **Java Backend**: Spring Boot 3.2 + MySQL + OpenAI GPT
- **Node.js Backend**: Express + PostgreSQL
- **Build Tools**: Gradle, NPM
- **Deployment**: AWS EC2

---

**🎉 완벽한 풀스택 문화행사 추천 플랫폼!**