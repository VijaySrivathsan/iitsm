@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-app.ps1"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application stopped with error code %ERRORLEVEL%.
    pause
)
