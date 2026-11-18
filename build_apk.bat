@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo        ULTRON Mobile - APK Build Automation
echo ========================================================

:: Check if JAVA_HOME is valid
if not defined JAVA_HOME (
    if exist "C:\Users\%USERNAME%\jdk17" (
        for /d %%i in ("C:\Users\%USERNAME%\jdk17\jdk*") do set "JAVA_HOME=%%i"
        if not defined JAVA_HOME set "JAVA_HOME=C:\Users\%USERNAME%\jdk17"
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17*" (
        for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%i"
    ) else if exist "C:\Program Files\Microsoft\jdk-17*" (
        for /d %%i in ("C:\Program Files\Microsoft\jdk-17*") do set "JAVA_HOME=%%i"
    ) else if exist "C:\Program Files\Java\jdk-17*" (
        for /d %%i in ("C:\Program Files\Java\jdk-17*") do set "JAVA_HOME=%%i"
    )
)

if defined JAVA_HOME (
    echo [*] Using JAVA_HOME: !JAVA_HOME!
    set "PATH=!JAVA_HOME!\bin;!PATH!"
) else (
    echo [!] WARNING: JAVA_HOME is not set. Looking for java in PATH...
    where java >nul 2>nul
    if errorlevel 1 (
        echo [X] ERROR: Java 17 is required to build Ultron APK.
        echo Please ensure JDK 17 is installed or run setup_env.bat.
        pause
        exit /b 1
    )
)

echo [*] Starting Gradle build for Ultron Debug APK...
call gradlew.bat assembleDebug --no-daemon

if errorlevel 1 (
    echo.
    echo [X] Build failed! Check the error log above.
    pause
    exit /b %errorlevel%
)

echo.
echo ========================================================
echo [OK] SUCCESS! ULTRON APK BUILT SUCCESSFULLY:
echo app\build\outputs\apk\debug\Ultron-v0.21.0-debug.apk
echo ========================================================
pause

