@echo off
setlocal EnableDelayedExpansion

:: 设置控制台编码为 UTF-8 (65001)
chcp 65001 >nul

set "ROOT=%~dp0"
set "APP_DIR=%ROOT%stzbhelper-java"

echo ==========================================
echo       stzbHelper Java 版 - 一键启动
echo ==========================================
echo [1/3] 正在检查环境...

:: 检查 gradlew.bat
if not exist "%APP_DIR%\gradlew.bat" (
    echo [错误] 找不到 gradlew.bat，请确保在项目根目录运行。
    pause
    exit /b 1
)

:: 检查 JAVA_HOME
if not defined JAVA_HOME (
    set "FALLBACK_JAVA_HOME=%USERPROFILE%\.jdks\temurin-21"
    if exist "!FALLBACK_JAVA_HOME!\bin\java.exe" (
        set "JAVA_HOME=!FALLBACK_JAVA_HOME!"
    )
)

if not defined JAVA_HOME (
    java -version >nul 2>&1
    if errorlevel 1 (
        echo [错误] 未找到 JDK 21，请参考 README 安装。
        pause
        exit /b 1
    )
)

:: 处理端口
set "PORT=%1"
if "%PORT%"=="" set "PORT=9527"

echo [2/3] 正在编译项目 (跳过测试)...
cd /d "%APP_DIR%"
call gradlew.bat build -x test >nul
if errorlevel 1 (
    echo [错误] 编译失败，请检查代码或 JDK 版本。
    pause
    exit /b 1
)

echo [3/3] 正在启动服务 (端口: %PORT%)...
echo ------------------------------------------
echo 访问地址: http://127.0.0.1:%PORT%
echo ------------------------------------------

:: 启动 Java 服务
call gradlew.bat run --args="%PORT%"

if errorlevel 1 (
    echo.
    echo [提示] 服务已停止。
    pause
)

endlocal
