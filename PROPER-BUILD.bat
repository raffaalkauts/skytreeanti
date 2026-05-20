@echo off
echo ============================================
echo Skytree PROPER BUILD with Compilation
echo ============================================
echo.

REM Setup paths
set "SRC=src\main\java"
set "RES=src\main\resources"
set "OUT=target\classes"
set "LIBS=libs\*"

REM Clean and create output
if exist "%OUT%" rmdir /S /Q "%OUT%"
mkdir "%OUT%"
if not exist "libs" mkdir "libs"

REM Download Paper API
if not exist "%LIBS%" (
    echo Downloading Paper API...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21-R0.1-SNAPSHOT/paper-api-1.21-R0.1-20240710.013912-161.jar' -OutFile '%LIBS%'" 2>nul
)

echo.
echo Compiling Java sources...
echo This may take a while...
echo.

REM Create source file list
REM Create source file list using relative paths to avoid escaping issues
powershell -Command "Resolve-Path '%SRC%\*' | Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName.Replace((Get-Location).Path + '\', '') } | Out-File -Encoding ASCII sources.txt"

REM Compile all at once
javac -cp "%LIBS%" -d "%OUT%" -encoding UTF-8 @"sources.txt" 2>compile_error.txt

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ============================================
    echo COMPILATION FAILED!
    echo ============================================
    echo.
    echo Showing errors:
    type compile_error.txt
    echo.
    echo ============================================
    echo SOLUTION: Build with IDE instead!
    echo ============================================
    echo.
    echo IntelliJ IDEA:
    echo   1. Build -> Build Project
    echo   2. Build -> Build Artifacts
    echo.
    echo Eclipse:
    echo   1. Project -> Build All  
    echo   2. File -> Export -> JAR file
    echo.
    del sources.txt
    exit /b 1
)

del sources.txt

echo Compilation successful!
echo.

REM Copy resources
echo Copying resources...
xcopy /Y /E "%RES%\*" "%OUT%\" >nul

REM Create JAR
echo Creating JAR...
cd "%OUT%"
jar cf ..\Skytree-v3.2.0.jar *
cd ..\..

if exist "target\Skytree-v3.2.0.jar" (
    echo.
    echo ============================================
    echo BUILD SUCCESS!
    echo ============================================
    echo.
    echo JAR: target\Skytree-v3.2.0.jar
    for %%I in (target\Skytree-v3.2.0.jar) do echo Size: %%~zI bytes
    echo.
    echo Ready to deploy to Paper server!
    echo.
) else (
    echo JAR creation failed!
    exit /b 1
)
