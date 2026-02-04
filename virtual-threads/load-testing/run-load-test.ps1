# Load Testing Script for Virtual Threads vs Platform Threads
# Requires: hey (install from https://github.com/rakyll/hey)
# Usage: .\run-load-test.ps1

param(
    [string]$ServerUrl = "http://localhost:8080",
    [int]$Connections = 1000,
    [int]$Requests = 10000,
    [int]$Duration = 30,
    [string]$TestName = "test-$(Get-Date -Format 'yyyy-MM-dd-HHmmss')"
)

# Create results directory if it doesn't exist
$resultsDir = ".\results"
if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir | Out-Null
}

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$resultFile = "$resultsDir\$TestName.txt"
$jsonFile = "$resultsDir\$TestName.json"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Load Testing with 'hey'" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Name: $TestName" -ForegroundColor Yellow
Write-Host "Server URL: $ServerUrl" -ForegroundColor Yellow
Write-Host "Connections: $Connections" -ForegroundColor Yellow
Write-Host "Total Requests: $Requests" -ForegroundColor Yellow
Write-Host "Duration: $Duration seconds" -ForegroundColor Yellow
Write-Host "Timestamp: $timestamp" -ForegroundColor Yellow
Write-Host ""

# Check if hey is installed
$heyPath = (Get-Command hey -ErrorAction SilentlyContinue).Source
if (-not $heyPath) {
    Write-Host "ERROR: 'hey' is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Install from: https://github.com/rakyll/hey" -ForegroundColor Red
    Write-Host ""
    Write-Host "On Windows, you can download the binary from releases" -ForegroundColor Yellow
    Write-Host "Then add it to your PATH or place it in the current directory" -ForegroundColor Yellow
    exit 1
}

Write-Host "Using hey from: $heyPath" -ForegroundColor Green
Write-Host ""

# Run the load test and capture output
Write-Host "Starting load test..." -ForegroundColor Cyan
Write-Host ""

# Run hey with specified parameters
hey -n $Requests -c $Connections -z "$Duration`s" -o $jsonFile $ServerUrl 2>&1 | Tee-Object -FilePath $resultFile

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Results saved to:"
Write-Host "  Text: $resultFile" -ForegroundColor Green
Write-Host "  JSON: $jsonFile" -ForegroundColor Green
Write-Host ""

# Parse and display key metrics
Write-Host "Key Metrics Summary:" -ForegroundColor Yellow
Write-Host ""

$content = Get-Content $resultFile -Raw
if ($content -match "Average:\s+([\d.]+)\s+secs") {
    Write-Host "  Average Response Time: $($matches[1]) seconds" -ForegroundColor Green
}
if ($content -match "Fastest:\s+([\d.]+)\s+secs") {
    Write-Host "  Fastest Response: $($matches[1]) seconds" -ForegroundColor Green
}
if ($content -match "Slowest:\s+([\d.]+)\s+secs") {
    Write-Host "  Slowest Response: $($matches[1]) seconds" -ForegroundColor Green
}
if ($content -match "Requests/sec:\s+([\d.]+)") {
    Write-Host "  Throughput: $($matches[1]) req/sec" -ForegroundColor Green
}
if ($content -match "Total data:\s+([\d.]+)\s+MB") {
    Write-Host "  Total Data Transferred: $($matches[1]) MB" -ForegroundColor Green
}

Write-Host ""
Write-Host "Use compare-results.py to compare multiple test runs" -ForegroundColor Cyan
