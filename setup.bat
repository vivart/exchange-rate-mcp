@echo off
echo === Exchange Rate MCP Server - Setup ===
echo.

REM Step 1: verify Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found on PATH.
    echo Please install JDK 21 from: https://adoptium.net/
    echo After installing, re-run this script.
    pause
    exit /b 1
)
echo [OK] Java found.

REM Step 2: generate Gradle wrapper (requires Gradle installed once)
if not exist "gradlew.bat" (
    gradle wrapper --gradle-version 8.13 >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] Gradle not found. Cannot generate wrapper.
        echo Option A: Install Gradle from https://gradle.org/install/ then re-run.
        echo Option B: Open this folder in IntelliJ IDEA - it handles Gradle automatically.
        pause
        exit /b 1
    )
    echo [OK] Gradle wrapper generated.
) else (
    echo [OK] Gradle wrapper already present.
)

REM Step 3: build fat JAR
echo.
echo Building JAR (first run downloads dependencies - may take a few minutes)...
call gradlew.bat shadowJar
if errorlevel 1 (
    echo [ERROR] Build failed. Check output above.
    pause
    exit /b 1
)

echo.
echo === Build successful! ===
echo JAR location: build\libs\exchange-rate-mcp.jar
echo.
echo To test manually:
echo   java -jar build\libs\exchange-rate-mcp.jar
echo.
echo To use with Claude Desktop, add to claude_desktop_config.json:
echo   See claude_desktop_config.example.json in this folder.
pause
