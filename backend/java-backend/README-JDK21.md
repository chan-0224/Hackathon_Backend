# 🚀 JDK 21 업그레이드 가이드

## 📋 개요

이 프로젝트는 JDK 21의 새로운 기능들을 활용하여 업그레이드되었습니다.

## ✨ JDK 21에서 활용한 새로운 기능들

### 1. **Record 클래스**
- 불변 데이터 객체를 간단하게 정의
- Lombok 의존성 제거 가능
- 자동으로 equals(), hashCode(), toString() 생성

```java
// 기존 (Lombok 사용)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ChatbotRequest {
    private String question;
}

// JDK 21 (Record 사용)
public record ChatbotRequest(String question) {}
```

### 2. **var 키워드 활용**
- 타입 추론을 통한 간결한 코드
- 지역 변수에서 타입 생략 가능

```java
// 기존
String[] tagArray = tags.split(",");
String firstTag = tagArray[0].trim();

// JDK 21
var tagArray = tags.split(",");
var firstTag = tagArray[0].trim();
```

### 3. **Text Blocks (JDK 15+)**
- 멀티라인 문자열을 더 읽기 쉽게 작성

```java
String prompt = """
    다음은 사용자의 질문입니다. 이 질문에서 '지역', '날짜', '행사 종류', '행사명'을 추출하여 JSON 객체로 반환해 주세요. 
    해당하는 키워드가 없으면 null로 표시하세요.
    
    사용자 질문: "%s"
    """;
```

## 🔧 빌드 설정 변경사항

### build.gradle
```gradle
java {
    sourceCompatibility = '21'
    targetCompatibility = '21'
}
```



### AWS 설정
- Elastic Beanstalk: Java 21 플랫폼 사용
- Lambda: java21 런타임 사용
- EC2: OpenJDK 21 설치

## 📦 변경된 파일들

### DTO 클래스들 (Record로 변경)
- `ChatbotRequest.java` → Record로 변경
- `ChatbotResponse.java` → Record로 변경
- `ApiResponse.java` → 새로운 Record 클래스 추가

### 서비스 클래스들
- `FestivalService.java` → var 키워드 활용
- `ChatbotService.java` → Record getter 메서드 호출 수정

### 컨트롤러들
- `FestivalController.java` → var 키워드 활용
- `HackathonApplication.java` → Record 활용한 에러 응답

## 🚀 배포 방법

### 1. EC2 직접 배포
```bash
# 환경 변수 설정
export EC2_HOST="your-ec2-ip"
export EC2_USER="ubuntu"
export EC2_KEY_PATH="~/.ssh/your-key.pem"

# 배포 실행
./deploy-ec2.sh
```

### 2. Elastic Beanstalk
```bash
# EB CLI 설치
pip install awsebcli

# 초기화 및 배포
eb init
eb create hackathon-backend
eb deploy
```



## 🔍 성능 개선사항

### 1. **메모리 사용량 최적화**
- Record 클래스로 인한 객체 생성 비용 감소
- 불변 객체로 인한 GC 부담 감소

### 2. **코드 가독성 향상**
- var 키워드로 인한 간결한 코드
- Record로 인한 명확한 데이터 구조

### 3. **유지보수성 향상**
- Lombok 의존성 제거
- 자동 생성 메서드로 인한 버그 가능성 감소

## ⚠️ 주의사항

### 1. **호환성**
- JDK 21 이상에서만 실행 가능
- Spring Boot 3.2.0 이상 필요

### 2. **Record 사용 시**
- getter 메서드는 필드명과 동일 (예: `request.question()`)
- 불변 객체이므로 setter 없음
- 생성자는 자동 생성

### 3. **var 사용 시**
- 지역 변수에서만 사용 가능
- 초기화와 함께 사용해야 함
- 복잡한 타입 추론은 피하는 것이 좋음

## 🧪 테스트

```bash
# 빌드 테스트
./gradlew clean build

# 로컬 실행 테스트
./gradlew bootRun


```

## 📚 참고 자료

- [JDK 21 릴리즈 노트](https://openjdk.org/projects/jdk/21/)
- [Record 클래스 가이드](https://docs.oracle.com/en/java/javase/21/language/records.html)
- [var 키워드 가이드](https://docs.oracle.com/en/java/javase/21/language/local-variable-type-inference.html)
