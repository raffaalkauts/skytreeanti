# Skytree Build Script (PowerShell version)
Write-Host "===================================" -ForegroundColor Cyan
Write-Host "Skytree Quick Build" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host ""

# Create output directories
if (!(Test-Path "target\classes")) { New-Item -ItemType Directory -Path "target\classes" -Force | Out-Null }
if (!(Test-Path "libs")) { New-Item -ItemType Directory -Path "libs" -Force | Out-Null }

# Check for dependencies
if (Test-Path "libs\paper-server.jar") {
    $classpath = "libs\paper-server.jar"
    Write-Host "Using local paper-server.jar" -ForegroundColor Green
}
else {
    Write-Host "[1/4] Downloading Paper API..." -ForegroundColor Yellow
    if (!(Test-Path "libs\paper-api.jar")) {
        try {
            Invoke-WebRequest -Uri "https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21-R0.1-SNAPSHOT/paper-api-1.21-R0.1-20240710.013912-161.jar" -OutFile "libs\paper-api.jar"
        }
        catch {
            Write-Host "Failed to download Paper API" -ForegroundColor Red
            exit 1
        }
    }
    $classpath = "libs\paper-api.jar"
}

# Find all Java files
Write-Host "[2/4] Compiling..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

# Compile
$compileArgs = @(
    "-cp", $classpath,
    "-d", "target\classes",
    "-sourcepath", "src\main\java",
    "-encoding", "UTF-8"
) + $javaFiles

& javac $compileArgs 2>&1 | Tee-Object -FilePath "compile.log"

if ($LASTEXITCODE -ne 0) {
    Write-Host "COMPILATION FAILED!" -ForegroundColor Red
    Get-Content "compile.log"
    exit 1
}

Write-Host "- Compiled successfully" -ForegroundColor Green

# Copy resources
Write-Host "[3/4] Copying resources..." -ForegroundColor Yellow
Copy-Item "src\main\resources\plugin.yml" "target\classes\" -Force
Copy-Item "src\main\resources\config.yml" "target\classes\" -Force
if (Test-Path "src\main\resources\mythic_items.json") {
    Copy-Item "src\main\resources\mythic_items.json" "target\classes\" -Force
}

# Create JAR
Write-Host "[4/4] Creating JAR..." -ForegroundColor Yellow
Push-Location "target\classes"
& jar cf "..\Skytree-v3.2.0.jar" *
Pop-Location

if (Test-Path "target\Skytree-v3.2.0.jar") {
    Write-Host ""
    Write-Host "===================================" -ForegroundColor Green
    Write-Host "SUCCESS!" -ForegroundColor Green
    Write-Host "===================================" -ForegroundColor Green
    $jarSize = (Get-Item "target\Skytree-v3.2.0.jar").Length
    Write-Host "JAR: target\Skytree-v3.2.0.jar" -ForegroundColor Cyan
    Write-Host "Size: $jarSize bytes" -ForegroundColor Cyan
    Write-Host ""
}
else {
    Write-Host "FAILED - JAR not created" -ForegroundColor Red
    exit 1
}
