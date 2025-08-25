#!/bin/bash

# =============================================================================
# EC2 직접 배포 스크립트 (Docker 없이)
# =============================================================================

set -e

echo "🚀 EC2 직접 배포 시작..."

# =============================================================================
# 환경 변수 확인
# =============================================================================
echo "📋 환경 변수 확인 중..."

required_vars=(
    "EC2_HOST"
    "EC2_USER"
    "EC2_KEY_PATH"
    "OPENAI_API_KEY"
    "SEOUL_CULTURE_API_KEY"
    "RDS_ENDPOINT"
    "RDS_USERNAME"
    "RDS_PASSWORD"
)

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ 필수 환경 변수가 설정되지 않았습니다: $var"
        exit 1
    fi
done

echo "✅ 모든 환경 변수가 설정되었습니다."

# =============================================================================
# 프로젝트 빌드
# =============================================================================
echo "🔨 프로젝트 빌드 중..."
./gradlew clean build -x test

if [ $? -ne 0 ]; then
    echo "❌ 빌드 실패"
    exit 1
fi

echo "✅ 빌드 완료"

# =============================================================================
# JAR 파일을 EC2로 전송
# =============================================================================
echo "📤 JAR 파일을 EC2로 전송 중..."
scp -i $EC2_KEY_PATH build/libs/*.jar $EC2_USER@$EC2_HOST:/opt/hackathon-backend/app.jar

if [ $? -ne 0 ]; then
    echo "❌ 파일 전송 실패"
    exit 1
fi

echo "✅ 파일 전송 완료"

# =============================================================================
# EC2에서 애플리케이션 실행
# =============================================================================
echo "🖥️ EC2에서 애플리케이션 실행 중..."

ssh -i $EC2_KEY_PATH $EC2_USER@$EC2_HOST << 'EOF'
    # 애플리케이션 디렉토리로 이동
    cd /opt/hackathon-backend
    
    # 기존 프로세스 종료
    if [ -f app.pid ]; then
        echo "기존 프로세스 종료 중..."
        kill $(cat app.pid) 2>/dev/null || true
        rm -f app.pid
    fi
    
    # 환경 변수 설정
    export SPRING_PROFILES_ACTIVE=prod
    export RDS_ENDPOINT=$RDS_ENDPOINT
    export RDS_USERNAME=$RDS_USERNAME
    export RDS_PASSWORD=$RDS_PASSWORD
    export OPENAI_API_KEY=$OPENAI_API_KEY
    export SEOUL_CULTURE_API_KEY=$SEOUL_CULTURE_API_KEY
    
    # JVM 옵션 설정
    export JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul"
    
    # 애플리케이션 백그라운드 실행
    echo "애플리케이션 시작 중..."
    nohup java $JAVA_OPTS -jar app.jar > app.log 2>&1 &
    echo $! > app.pid
    
    # 프로세스 시작 대기
    sleep 10
    
    # 헬스 체크
    for i in {1..30}; do
        if curl -f http://localhost:8080/api/festivals/status > /dev/null 2>&1; then
            echo "✅ 애플리케이션이 성공적으로 시작되었습니다!"
            break
        fi
        echo "헬스 체크 중... ($i/30)"
        sleep 2
    done
    
    if [ $i -eq 30 ]; then
        echo "❌ 애플리케이션 시작 실패"
        exit 1
    fi
EOF

if [ $? -eq 0 ]; then
    echo "✅ 배포 완료!"
    echo "🌐 애플리케이션 URL: http://$EC2_HOST:8080"
else
    echo "❌ 배포 실패"
    exit 1
fi

echo "🎉 EC2 배포가 성공적으로 완료되었습니다!"
