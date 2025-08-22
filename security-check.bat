@echo off
REM =============================================================================
REM 보안 검사 스크립트 (Windows)
REM =============================================================================
REM 이 스크립트는 GitHub에 업로드하기 전에 보안 문제를 검사합니다.
REM 
REM 사용법:
REM security-check.bat

echo 🔒 보안 검사를 시작합니다...

REM 검사 결과 카운터
set ERRORS=0
set WARNINGS=0

echo 📋 1. API 키 하드코딩 검사...

REM OpenAI API 키 검사
findstr /s /i "sk-proj-" *.java *.properties *.md 2>nul
if %errorlevel% equ 0 (
    echo ❌ 오류: OpenAI API 키가 하드코딩되어 있습니다!
    echo    다음 파일들을 확인하세요:
    findstr /s /i /l "sk-proj-" *.java *.properties *.md
    set /a ERRORS+=1
) else (
    echo ✅ OpenAI API 키 하드코딩 없음
)

REM 데이터베이스 비밀번호 검사
echo 📋 2. 데이터베이스 비밀번호 검사...

findstr /s /i "password=0000" *.properties 2>nul
if %errorlevel% equ 0 (
    echo ⚠️  경고: 기본 데이터베이스 비밀번호가 사용되고 있습니다.
    echo    운영환경에서는 강력한 비밀번호를 사용하세요.
    set /a WARNINGS+=1
) else (
    echo ✅ 데이터베이스 비밀번호 안전
)

REM 민감한 파일 검사
echo 📋 3. 민감한 파일 검사...

if exist "src\main\resources\application-local.properties" (
    echo ❌ 오류: application-local.properties 파일이 존재합니다!
    echo    이 파일은 .gitignore에 포함되어야 하며 GitHub에 업로드되면 안 됩니다.
    set /a ERRORS+=1
) else (
    echo ✅ application-local.properties 파일 없음
)

REM 환경변수 파일 검사
if exist ".env" (
    echo ❌ 오류: .env 파일이 존재합니다!
    set /a ERRORS+=1
) else (
    echo ✅ .env 파일 없음
)

REM .gitignore 검사
echo 📋 4. .gitignore 설정 검사...

findstr /i "application-local.properties" .gitignore >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ application-local.properties가 .gitignore에 포함됨
) else (
    echo ❌ 오류: application-local.properties가 .gitignore에 포함되지 않음
    set /a ERRORS+=1
)

findstr /i "\.env" .gitignore >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ .env 파일들이 .gitignore에 포함됨
) else (
    echo ❌ 오류: .env 파일들이 .gitignore에 포함되지 않음
    set /a ERRORS+=1
)

REM 로그 파일 검사
echo 📋 5. 로그 파일 검사...

dir /s /b *.log 2>nul | findstr /v "\.git" | findstr /v "build" >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚠️  경고: 로그 파일이 발견되었습니다.
    echo    로그 파일들:
    dir /s /b *.log 2>nul | findstr /v "\.git" | findstr /v "build"
    set /a WARNINGS+=1
) else (
    echo ✅ 로그 파일 없음
)

REM 결과 출력
echo.
echo ==============================================================================
echo 🔍 보안 검사 결과
echo ==============================================================================

if %ERRORS% equ 0 (
    if %WARNINGS% equ 0 (
        echo 🎉 모든 검사를 통과했습니다! GitHub에 안전하게 업로드할 수 있습니다.
        exit /b 0
    ) else (
        echo ⚠️  경고: %WARNINGS%개의 경고가 있습니다.
        echo    위의 경고 사항들을 확인하고 수정하는 것을 권장합니다.
        exit /b 0
    )
) else (
    echo ❌ 오류: %ERRORS%개의 오류와 %WARNINGS%개의 경고가 있습니다.
    echo    위의 오류들을 수정한 후 다시 검사를 실행하세요.
    exit /b 1
)
