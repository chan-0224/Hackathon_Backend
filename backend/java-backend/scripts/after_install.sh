#!/bin/bash

# =============================================================================
# CodeDeploy AfterInstall 스크립트
# =============================================================================

echo "🔧 AfterInstall 스크립트 실행 중..."

# 애플리케이션 디렉토리로 이동
cd /opt/hackathon-backend

# JAR 파일 권한 설정
chmod +x *.jar

# 환경 변수 파일 생성
cat > .env << EOF
SPRING_PROFILES_ACTIVE=prod
RDS_ENDPOINT=${RDS_ENDPOINT}
RDS_USERNAME=${RDS_USERNAME}
RDS_PASSWORD=${RDS_PASSWORD}
OPENAI_API_KEY=${OPENAI_API_KEY}
SEOUL_CULTURE_API_KEY=${SEOUL_CULTURE_API_KEY}
JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul"
EOF

echo "✅ AfterInstall 완료"
