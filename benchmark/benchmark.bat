@echo off
setlocal
cd /d "%~dp0"
set "K6=C:\Program Files\k6\k6.exe"

if "%~1"=="" (
    echo Usage: benchmark.bat SCALE RPS
    echo Example:
    echo benchmark.bat 5 500
    echo benchmark.bat 10 1000
    exit /b 1
)

if "%~2"=="" (
    echo Usage: benchmark.bat SCALE RPS
    exit /b 1
)

set SCALE=%1
set RATE=%2

if not exist benchmark-results.csv (
    echo Scale,RPS,P95ms,P99ms>benchmark-results.csv
)

echo.
echo ====================================================
echo Distributed Rate Limiter Benchmark
echo ====================================================
echo Scale      : %SCALE%
echo Target RPS : %RATE%
echo ====================================================
echo.

if exist result.json del /f /q result.json

echo.
echo Running k6 benchmark...
echo.

"%K6%" run -e RATE=%RATE% load-test.js --summary-export=result.json

if errorlevel 1 (
    echo.
    echo k6 execution failed.
    exit /b 1
)

if not exist result.json (
    echo result.json was not generated.
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "$json = Get-Content 'result.json' -Raw | ConvertFrom-Json; $rps=[math]::Round($json.metrics.http_reqs.rate,2); $p95=[math]::Round($json.metrics.http_req_duration.values.'p(95)',2); $p99=[math]::Round($json.metrics.http_req_duration.values.'p(99)',2); Add-Content 'benchmark-results.csv' ('%SCALE%,'+$rps+','+$p95+','+$p99)"

echo.
echo ====================================================
echo Results appended successfully
echo ====================================================
echo.

type benchmark-results.csv

echo.
echo Done.

exit /b 0