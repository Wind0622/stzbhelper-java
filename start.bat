@echo off
setlocal EnableDelayedExpansion

set "ROOT=%~dp0"
set "APP_DIR=%ROOT%stzbhelper-java"

if not exist "%APP_DIR%\\gradlew.bat" (
  echo ERROR: gradlew.bat not found in %APP_DIR%
  exit /b 1
)

if not defined JAVA_HOME (
  set "FALLBACK_JAVA_HOME=%USERPROFILE%\\.jdks\\temurin-21"
  if exist "%FALLBACK_JAVA_HOME%\\bin\\java.exe" (
    set "JAVA_HOME=%FALLBACK_JAVA_HOME%"
  )
)

set "PORT=%1"
if "%PORT%"=="" set "PORT=9527"

set "FOUND_PORT="
for /l %%p in (0,1,20) do (
  set /a "TRYPORT=PORT+%%p"
  call :isPortFree !TRYPORT! IS_FREE
  if "!IS_FREE!"=="1" (
    set "FOUND_PORT=!TRYPORT!"
    goto :portFound
  )
)

echo ERROR: No free port found in range %PORT%-%PORT%+20
exit /b 1

:portFound
if not "%FOUND_PORT%"=="%PORT%" (
  echo Port %PORT% is in use, switching to %FOUND_PORT%...
)
set "PORT=%FOUND_PORT%"

cd /d "%APP_DIR%"

echo Building...
call gradlew.bat build -x test
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

echo Starting on port %PORT%...
call gradlew.bat run --args="%PORT%"

endlocal

:isPortFree
set "CHECKPORT=%~1"
set "FREE=1"
for /f "tokens=*" %%A in ('netstat -ano ^| findstr /R /C:":%CHECKPORT% " ^| findstr /C:"LISTENING" 2^>NUL') do set "FREE=0"
set "%~2=%FREE%"
exit /b 0
