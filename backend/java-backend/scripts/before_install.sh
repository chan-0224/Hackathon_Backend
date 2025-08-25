#!/bin/bash

# =============================================================================
# CodeDeploy BeforeInstall 스크립트
# =============================================================================

echo "🔧 BeforeInstall 스크립트 실행 중..."

# Java 21 설치 (이미 설치되어 있지 않은 경우)
if ! command -v java &> /dev/null; then
    echo "Java 21 설치 중..."
    sudo apt-get update
    sudo apt-get install -y openjdk-21-jdk
else
    # Java 버전 확인
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt "21" ]; then
        echo "Java 21로 업그레이드 중..."
        sudo apt-get update
        sudo apt-get install -y openjdk-21-jdk
    fi
fi

# 애플리케이션 디렉토리 생성
sudo mkdir -p /opt/hackathon-backend
sudo chown ubuntu:ubuntu /opt/hackathon-backend

# 로그 디렉토리 생성
sudo mkdir -p /var/log/hackathon-backend
sudo chown ubuntu:ubuntu /var/log/hackathon-backend

echo "✅ BeforeInstall 완료"
