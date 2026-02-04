@echo off
REM Quick shortcuts for different Gatling simulations
REM Usage examples:
REM   stress-test.bat          - Runs stress test simulation
REM   stress-test.bat users=500 duration=120

setlocal enabledelayedexpansion

set JAVA_HOME=C:\Users\gamba\.jdks\corretto-25.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

echo ==========================================
echo Gatling Stress Test
echo ==========================================
echo.
echo This test progressively increases load to find breaking point
echo.

.\mvnw clean compile exec:java ^
  -Dexec.mainClass="com.example.gatling.LoadTestRunner" ^
  -Dusers=200 ^
  -DrampUp=90 ^
  -Dduration=300

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✓ Stress test completed!
    echo Opening report...
    for /f "delims=" %%D in ('dir /b /od target\gatling\results\') do set LATEST=%%D
    if not "!LATEST!"=="" (
        timeout /t 2 /nobreak
        start target\gatling\results\!LATEST!\index.html
    )
)

pause
endlocal
