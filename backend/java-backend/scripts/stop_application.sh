#!/bin/bash

# =============================================================================
# CodeDeploy ApplicationStop 스크립트
# =============================================================================

echo "🛑 애플리케이션 중지 중..."

# 애플리케이션 디렉토리로 이동
cd /opt/hackathon-backend

# 기존 프로세스 종료
if [ -f app.pid ]; then
    echo "프로세스 종료 중..."
    kill $(cat app.pid) 2>/dev/null || true
    rm -f app.pid
    echo "✅ 애플리케이션이 중지되었습니다."
else
    echo "실행 중인 프로세스가 없습니다."
fi
