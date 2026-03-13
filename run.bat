@echo off
setlocal EnableDelayedExpansion

:: --- PRE-START: Set encoding to UTF-8 for better Chinese display ---
chcp 65001 >nul

set "ROOT=%~dp0"
set "APP_DIR=%ROOT%stzbhelper-java"

echo ==========================================
echo       stzbHelper Java One-Click Run
echo ==========================================
echo [1/3] Checking environment...

:: Check gradlew.bat existence
if not exist "%APP_DIR%\gradlew.bat" (
    echo [ERROR] gradlew.bat not found in %APP_DIR%
    pause
    exit /b 1
)

:: Try to resolve JAVA_HOME if not set
if not defined JAVA_HOME (
    set "FALLBACK_JAVA_HOME=%USERPROFILE%\.jdks\temurin-21"
    if exist "!FALLBACK_JAVA_HOME!\bin\java.exe" (
        set "JAVA_HOME=!FALLBACK_JAVA_HOME!"
    )
)

:: Verify Java exists
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] JDK not found. Please check README for installation steps.
    pause
    exit /b 1
)

:: Port configuration
set "PORT=%1"
if "%PORT%"=="" set "PORT=9527"

echo [2/3] Building project (Skipping tests)...
cd /d "%APP_DIR%"
call gradlew.bat build -x test >nul
if errorlevel 1 (
    echo.
    echo [ERROR] Build failed. Try running manually:
    echo cd stzbhelper-java ; ./gradlew.bat build
    pause
    exit /b 1
)

echo [3/3] Starting service on port %PORT%...
echo ------------------------------------------
echo URL: http://127.0.0.1:%PORT%
echo ------------------------------------------

:: Set Gradle options for plain console and UTF-8
set "GRADLE_OPTS=-Dfile.encoding=UTF-8 -Dorg.gradle.console=plain"

:: Execute Gradle Run
call gradlew.bat run --args="%PORT%"

if errorlevel 1 (
    echo.
    echo [INFO] Service stopped.
    pause
)

endlocal
