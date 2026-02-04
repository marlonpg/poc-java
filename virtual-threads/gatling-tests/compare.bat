@echo off
REM Helper script to run specific load tests for comparing Virtual Threads vs Platform Threads
REM This makes it easy to run consistent tests

setlocal enabledelayedexpansion

set JAVA_HOME=C:\Users\gamba\.jdks\corretto-25.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

echo ==========================================
echo Virtual Threads vs Platform Threads
echo Comparison Test Suite
echo ==========================================
echo.
echo Usage: compare.bat [test_name] [options]
echo.
echo Available tests:
echo   light       - 100 users, 60 sec ramp-up, 300 sec duration
echo   medium      - 500 users, 120 sec ramp-up, 300 sec duration  
echo   heavy       - 1000 users, 60 sec ramp-up, 300 sec duration
echo   extreme     - 2000 users, 60 sec ramp-up, 300 sec duration
echo.
echo Options: baseurl=http://...
echo.

REM Default test
set TEST_NAME=%1
if "!TEST_NAME!"=="" (
    set TEST_NAME=medium
)

set USERS=500
set RAMP_UP=120
set DURATION=300

if "!TEST_NAME!"=="light" (
    set USERS=100
    set RAMP_UP=60
) else if "!TEST_NAME!"=="medium" (
    set USERS=500
    set RAMP_UP=120
) else if "!TEST_NAME!"=="heavy" (
    set USERS=1000
    set RAMP_UP=60
) else if "!TEST_NAME!"=="extreme" (
    set USERS=2000
    set RAMP_UP=60
)

echo Running: !TEST_NAME! test
echo Users: !USERS!, Ramp-up: !RAMP_UP! sec, Duration: !DURATION! sec
echo.

mvnw clean compile exec:java ^
  -Dexec.mainClass="com.example.gatling.LoadTestRunner" ^
  -Dusers=!USERS! ^
  -DrampUp=!RAMP_UP! ^
  -Dduration=!DURATION!

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✓ Test completed successfully!
    echo.
    echo Next steps:
    echo 1. Note the results from this run
    echo 2. Switch server to Platform Threads:
    echo    - Edit ..\VirtualThreadWebServer.java
    echo    - Change: newVirtualThreadPerTaskExecutor()
    echo    - To: newFixedThreadPool(200)
    echo 3. Restart the server
    echo 4. Run this script again with the same test
    echo 5. Compare the HTML reports in target\gatling\results\
    echo.
    echo Opening report...
    for /f "delims=" %%D in ('dir /b /od target\gatling\results\') do set LATEST=%%D
    if not "!LATEST!"=="" (
        timeout /t 2 /nobreak
        start target\gatling\results\!LATEST!\index.html
    )
)

pause
endlocal
