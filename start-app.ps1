# start-app.ps1 - Launch I-ITSM Application
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "       Starting I-ITSM Application (Spring Boot)       " -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

# Check if Maven is available on PATH
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "Maven detected on PATH! Running via 'mvn spring-boot:run'..." -ForegroundColor Green
    mvn spring-boot:run
    exit $LASTEXITCODE
}

# Find Java runtime and compiler
$java = "java"
$javac = "javac"
if (Test-Path "C:\Program Files\Java\jdk-26.0.2.1\bin\java.exe") {
    $java = "C:\Program Files\Java\jdk-26.0.2.1\bin\java.exe"
    $javac = "C:\Program Files\Java\jdk-26.0.2.1\bin\javac.exe"
} else {
    $jdk = Get-ChildItem "C:\Program Files\Java" -Filter "jdk*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) {
        $java = Join-Path $jdk.FullName "bin\java.exe"
        $javac = Join-Path $jdk.FullName "bin\javac.exe"
    }
}

# Verify Maven local repository cache
$m2Repo = "$HOME\.m2\repository"
if (!(Test-Path $m2Repo)) {
    Write-Host "Error: Maven cache not found at $m2Repo." -ForegroundColor Red
    Write-Host "Please ensure Maven dependencies are present." -ForegroundColor Yellow
    exit 1
}

$targetDir = "$ScriptDir\target.nosync"
if (!(Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

$classesDir = "$targetDir\classes"
if (!(Test-Path $classesDir)) {
    New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
}

$jars = (Get-ChildItem -Path $m2Repo -Filter "*.jar" -Recurse | Where-Object { $_.FullName -notmatch "1\.7\.36" } | Select-Object -ExpandProperty FullName) -join ";"
$jarsForward = $jars.Replace("\", "/")
$classesDirForward = $classesDir.Replace("\", "/")
$javaFiles = (Get-ChildItem -Path "$ScriptDir\src\main\java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName)

Write-Host "1/3. Compiling Java sources with JDK 21+ compatibility..." -ForegroundColor Gray
$sbJavac = New-Object System.Text.StringBuilder
[void]$sbJavac.AppendLine("--release 21")
[void]$sbJavac.AppendLine("-parameters")
[void]$sbJavac.AppendLine("-d `"$classesDirForward`"")
[void]$sbJavac.AppendLine("-cp `"$classesDirForward;$jarsForward`"")
foreach ($f in $javaFiles) {
    [void]$sbJavac.AppendLine('"' + $f.Replace("\", "/") + '"')
}

$tempDir = [System.IO.Path]::GetTempPath()
$javacArgsFile = Join-Path $tempDir "iitsm_javac_args.txt"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($javacArgsFile, $sbJavac.ToString(), $utf8NoBom)

& $javac "@$javacArgsFile"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed with exit code $LASTEXITCODE." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "2/3. Synchronizing resources and templates..." -ForegroundColor Gray
Copy-Item -Path "$ScriptDir\src\main\resources\*" -Destination $classesDir -Recurse -Force

Write-Host "3/3. Launching Spring Boot server..." -ForegroundColor Gray
Write-Host ""
Write-Host "=======================================================" -ForegroundColor Green
Write-Host " I-ITSM Application is starting!" -ForegroundColor Green
Write-Host " URL: http://localhost:8080" -ForegroundColor Green
Write-Host " Demo Logins:" -ForegroundColor Yellow
Write-Host "   - Admin:      admin / demo123" -ForegroundColor Yellow
Write-Host "   - Technician: tech1 / demo123" -ForegroundColor Yellow
Write-Host "   - Employee:   emp1  / demo123" -ForegroundColor Yellow
Write-Host " Press Ctrl+C in this console to stop the server." -ForegroundColor Gray
Write-Host "=======================================================" -ForegroundColor Green
Write-Host ""

# Background job to automatically open the browser once the app responds
Start-Job -ScriptBlock {
    Start-Sleep -Seconds 3
    for ($i = 0; $i -lt 20; $i++) {
        try {
            $resp = Invoke-WebRequest -Uri "http://localhost:8080" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($resp -and ($resp.StatusCode -eq 200 -or $resp.StatusCode -eq 302)) {
                Start-Process "http://localhost:8080"
                break
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
} | Out-Null

$sbJava = New-Object System.Text.StringBuilder
[void]$sbJava.AppendLine("-Dspring.classformat.ignore=true")
[void]$sbJava.AppendLine("-cp")
[void]$sbJava.AppendLine('"' + $classesDirForward + ';' + $jarsForward + '"')
[void]$sbJava.AppendLine("com.iitsm.ItsmApplication")

$javaArgsFile = Join-Path $tempDir "iitsm_java_args.txt"
[System.IO.File]::WriteAllText($javaArgsFile, $sbJava.ToString(), $utf8NoBom)

& $java "@$javaArgsFile"
