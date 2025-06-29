@echo off
REM Build script for Flink Anomaly Detection Job (Windows)
REM Usage: build.bat [clean|package|run|test|docker]

set PROJECT_DIR=%~dp0
set JAR_NAME=flink-river-anomaly-1.0-SNAPSHOT.jar
set TARGET_JAR=%PROJECT_DIR%target\%JAR_NAME%

if "%1"=="" set ACTION=package
if not "%1"=="" set ACTION=%1

if "%ACTION%"=="clean" (
    echo 🧹 Cleaning project...
    call mvn clean
    goto :eof
)

if "%ACTION%"=="package" (
    echo 📦 Building project...
    call mvn clean package -DskipTests
    
    if exist "%TARGET_JAR%" (
        echo ✅ Build successful: %TARGET_JAR%
        for %%I in ("%TARGET_JAR%") do echo 📊 JAR size: %%~zI bytes
    ) else (
        echo ❌ Build failed
        exit /b 1
    )
    goto :eof
)

if "%ACTION%"=="run" (
    echo 🚀 Running Flink job locally...
    
    REM Build if needed
    if not exist "%TARGET_JAR%" (
        echo 📦 JAR not found, building first...
        call mvn clean package -DskipTests
    )
    
    REM Set default values
    if "%RIVER_API_URL%"=="" set RIVER_API_URL=http://localhost:8000
    if "%DATA_PATH%"=="" set DATA_PATH=..\data\transactions.csv
    if "%DELAY_MS%"=="" set DELAY_MS=2000
    
    echo 🔍 Using River API at %RIVER_API_URL%
    echo 📁 Using data file: %DATA_PATH%
    echo ⏱️ Delay between records: %DELAY_MS%ms
    
    REM Run the job
    java -Xmx1g ^
         -Dlog4j.logger.com.example.anomaly=DEBUG ^
         -jar "%TARGET_JAR%" ^
         --river-api-url %RIVER_API_URL% ^
         --data-path %DATA_PATH% ^
         --delay-ms %DELAY_MS%
    goto :eof
)

if "%ACTION%"=="test" (
    echo 🧪 Running tests...
    call mvn test
    goto :eof
)

if "%ACTION%"=="docker" (
    echo 🐳 Building Docker image...
    docker build -t flink-anomaly-job .
    echo ✅ Docker image built: flink-anomaly-job
    goto :eof
)

echo Usage: %0 [clean^|package^|run^|test^|docker]
echo.
echo Commands:
echo   clean   - Clean build artifacts
echo   package - Build the JAR file (default)
echo   run     - Build and run locally
echo   test    - Run unit tests
echo   docker  - Build Docker image
echo.
echo Environment variables for 'run' command:
echo   RIVER_API_URL - River API URL (default: http://localhost:8000)
echo   DATA_PATH     - Path to CSV file (default: ..\data\transactions.csv)
echo   DELAY_MS      - Delay between records (default: 2000)
