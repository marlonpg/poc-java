@echo off
REM Quick shortcuts for sustained load test simulation
REM Usage examples:
REM   sustained-load.bat
REM   sustained-load.bat baseurl=http://localhost:9090

setlocal enabledelayedexpansion

set JAVA_HOME=C:\Users\gamba\.jdks\corretto-25.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

echo ==========================================
echo Gatling Sustained Load Test
echo ==========================================
echo.
echo This test maintains 500 users for 10 minutes
echo Useful for measuring stability and consistency
echo.

.\mvnw clean compile exec:java ^
  -Dexec.mainClass="com.example.gatling.LoadTestRunner" ^
  -Dusers=500 ^
  -DrampUp=60 ^
  -Dduration=600

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✓ Sustained load test completed!
    echo Opening report...
    for /f "delims=" %%D in ('dir /b /od target\gatling\results\') do set LATEST=%%D
    if not "!LATEST!"=="" (
        timeout /t 2 /nobreak
        start target\gatling\results\!LATEST!\index.html
    )
)

pause
endlocal
