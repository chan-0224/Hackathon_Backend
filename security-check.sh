#!/bin/bash

# =============================================================================
# 보안 검사 스크립트
# =============================================================================
# 이 스크립트는 GitHub에 업로드하기 전에 보안 문제를 검사합니다.
# 
# 사용법:
# chmod +x security-check.sh
# ./security-check.sh

echo "🔒 보안 검사를 시작합니다..."

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 검사 결과 카운터
ERRORS=0
WARNINGS=0

echo "📋 1. API 키 하드코딩 검사..."

# OpenAI API 키 검사
if grep -r "sk-proj-" . --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle; then
    echo -e "${RED}❌ 오류: OpenAI API 키가 하드코딩되어 있습니다!${NC}"
    echo "   다음 파일들을 확인하세요:"
    grep -r "sk-proj-" . --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle -l
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ OpenAI API 키 하드코딩 없음${NC}"
fi

# 데이터베이스 비밀번호 검사
echo "📋 2. 데이터베이스 비밀번호 검사..."

if grep -r "password=0000" . --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle; then
    echo -e "${YELLOW}⚠️  경고: 기본 데이터베이스 비밀번호가 사용되고 있습니다.${NC}"
    echo "   운영환경에서는 강력한 비밀번호를 사용하세요."
    WARNINGS=$((WARNINGS + 1))
else
    echo -e "${GREEN}✅ 데이터베이스 비밀번호 안전${NC}"
fi

# 민감한 파일 검사
echo "📋 3. 민감한 파일 검사..."

if [ -f "src/main/resources/application-local.properties" ]; then
    echo -e "${RED}❌ 오류: application-local.properties 파일이 존재합니다!${NC}"
    echo "   이 파일은 .gitignore에 포함되어야 하며 GitHub에 업로드되면 안 됩니다."
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ application-local.properties 파일 없음${NC}"
fi

# 환경변수 파일 검사
if [ -f ".env" ] || [ -f ".env.local" ] || [ -f ".env.production" ]; then
    echo -e "${RED}❌ 오류: .env 파일이 존재합니다!${NC}"
    echo "   다음 파일들을 확인하세요:"
    ls -la .env* 2>/dev/null || true
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ .env 파일 없음${NC}"
fi

# .gitignore 검사
echo "📋 4. .gitignore 설정 검사..."

if grep -q "application-local.properties" .gitignore; then
    echo -e "${GREEN}✅ application-local.properties가 .gitignore에 포함됨${NC}"
else
    echo -e "${RED}❌ 오류: application-local.properties가 .gitignore에 포함되지 않음${NC}"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "\.env" .gitignore; then
    echo -e "${GREEN}✅ .env 파일들이 .gitignore에 포함됨${NC}"
else
    echo -e "${RED}❌ 오류: .env 파일들이 .gitignore에 포함되지 않음${NC}"
    ERRORS=$((ERRORS + 1))
fi

# 로그 파일 검사
echo "📋 5. 로그 파일 검사..."

if find . -name "*.log" -not -path "./.git/*" -not -path "./build/*" | head -1 | grep -q .; then
    echo -e "${YELLOW}⚠️  경고: 로그 파일이 발견되었습니다.${NC}"
    echo "   로그 파일들:"
    find . -name "*.log" -not -path "./.git/*" -not -path "./build/*"
    WARNINGS=$((WARNINGS + 1))
else
    echo -e "${GREEN}✅ 로그 파일 없음${NC}"
fi

# 결과 출력
echo ""
echo "=============================================================================="
echo "🔍 보안 검사 결과"
echo "=============================================================================="

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}🎉 모든 검사를 통과했습니다! GitHub에 안전하게 업로드할 수 있습니다.${NC}"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  경고: $WARNINGS개의 경고가 있습니다.${NC}"
    echo "   위의 경고 사항들을 확인하고 수정하는 것을 권장합니다."
    exit 0
else
    echo -e "${RED}❌ 오류: $ERRORS개의 오류와 $WARNINGS개의 경고가 있습니다.${NC}"
    echo "   위의 오류들을 수정한 후 다시 검사를 실행하세요."
    exit 1
fi
