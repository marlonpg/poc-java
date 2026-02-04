@echo off
setlocal enabledelayedexpansion

set JAVA_HOME=C:\Users\gamba\.jdks\corretto-25.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

echo ==========================================
echo Load Test Suite
echo ==========================================
echo.

REM Default parameters
set BASE_URL=http://localhost:8080
set USERS=100
set RAMP_UP=60
set DURATION=300

REM Check if user provided arguments
if "%1"=="" (
    echo Using default parameters
    echo Users: %USERS%, Ramp-up: %RAMP_UP% sec, Duration: %DURATION% sec
    echo.
) else (
    REM Parse command line arguments
    for %%A in (%*) do (
        if "%%A:~0,6%"=="users=" (
            set USERS=%%A:~6%
        ) else if "%%A:~0,7%"=="rampup=" (
            set RAMP_UP=%%A:~7%
        ) else if "%%A:~0,9%"=="duration=" (
            set DURATION=%%A:~9%
        ) else if "%%A:~0,8%"=="baseurl=" (
            set BASE_URL=%%A:~8%
        )
    )
)

echo Base URL: %BASE_URL%
echo Users: %USERS%
echo Ramp-up Duration: %RAMP_UP% seconds
echo Test Duration: %DURATION% seconds
echo.
echo ==========================================
echo Starting load test...
echo ==========================================
echo.

REM Run custom load test with parameters
.\mvnw clean compile exec:java ^
  -Dexec.mainClass="com.example.gatling.LoadTestRunner" ^
  -DbaseUrl=%BASE_URL% ^
  -Dusers=%USERS% ^
  -DrampUp=%RAMP_UP% ^
  -Dduration=%DURATION%

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==========================================
    echo ✓ Test completed successfully!
    echo ==========================================
    echo.
) else (
    echo.
    echo ==========================================
    echo ✗ Test failed!
    echo ==========================================
    echo Check the output above for errors.
)

echo.
pause
endlocal

echo.
pause
endlocal
