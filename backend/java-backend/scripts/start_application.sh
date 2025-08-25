#!/bin/bash

# =============================================================================
# CodeDeploy ApplicationStart 스크립트
# =============================================================================

echo "🚀 애플리케이션 시작 중..."

# 애플리케이션 디렉토리로 이동
cd /opt/hackathon-backend

# 환경 변수 로드
source .env

# 기존 프로세스 종료
if [ -f app.pid ]; then
    echo "기존 프로세스 종료 중..."
    kill $(cat app.pid) 2>/dev/null || true
    rm -f app.pid
fi

# 애플리케이션 시작
echo "새 애플리케이션 시작 중..."
nohup java $JAVA_OPTS -jar *.jar > /var/log/hackathon-backend/app.log 2>&1 &
echo $! > app.pid

# 시작 대기
sleep 10

# 헬스 체크
for i in {1..30}; do
    if curl -f http://localhost:8080/api/festivals/status > /dev/null 2>&1; then
        echo "✅ 애플리케이션이 성공적으로 시작되었습니다!"
        exit 0
    fi
    echo "헬스 체크 중... ($i/30)"
    sleep 2
done

echo "❌ 애플리케이션 시작 실패"
exit 1
