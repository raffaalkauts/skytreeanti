@echo off
setlocal enabledelayedexpansion
echo ============================================
echo Skytree File-by-File Compiler
echo ============================================
echo.

REM Setup
set "LIBS=libs\paper-api.jar"
set "OUT=target\classes"
set "ERROR_COUNT=0"
set "SUCCESS_COUNT=0"

if not exist "%OUT%" mkdir "%OUT%"
if not exist "libs" mkdir "libs"

REM Download Paper API if needed
if not exist "%LIBS%" (
    echo Downloading Paper API...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21-R0.1-SNAPSHOT/paper-api-1.21-R0.1-20240710.013912-161.jar' -OutFile 'libs\paper-api.jar'" 2>nul
    if !ERRORLEVEL! NEQ 0 (
        echo WARNING: Paper API download failed, will compile without it
    )
)

echo.
echo Starting compilation...
echo.

REM Compile files one by one
for /R "src\main\java" %%F in (*.java) do (
    set "FILE=%%F"
    
    REM Show progress
    echo Compiling: %%~nxF
    
    REM Compile
    if exist "%LIBS%" (
        javac -cp "%LIBS%;%OUT%" -d "%OUT%" -encoding UTF-8 "%%F" 2>temp_error.txt
    ) else (
        javac -cp "%OUT%" -d "%OUT%" -encoding UTF-8 "%%F" 2>temp_error.txt
    )
    
    if !ERRORLEVEL! EQU 0 (
        set /a SUCCESS_COUNT+=1
    ) else (
        set /a ERROR_COUNT+=1
        echo [ERROR] Failed to compile: %%~nxF
        type temp_error.txt
        echo.
    )
    
    del temp_error.txt 2>nul
)

echo.
echo ============================================
echo Compilation Summary
echo ============================================
echo Success: !SUCCESS_COUNT! files
echo Errors:  !ERROR_COUNT! files
echo.

if !ERROR_COUNT! GTR 0 (
    echo BUILD FAILED!
    exit /b 1
)

REM Copy resources
echo Copying resources...
copy /Y "src\main\resources\plugin.yml" "%OUT%\" >nul 2>&1
if exist "src\main\resources\config.yml" copy /Y "src\main\resources\config.yml" "%OUT%\" >nul 2>&1
if exist "src\main\resources\shop.yml" copy /Y "src\main\resources\shop.yml" "%OUT%\" >nul 2>&1

REM Create JAR
echo Creating JAR file...
cd "%OUT%"
jar cf ..\Skytree-v3.2.0.jar * 2>nul
cd ..\..

if exist "target\Skytree-v3.2.0.jar" (
    echo.
    echo ============================================
    echo BUILD SUCCESSFUL!
    echo ============================================
    echo.
    echo JAR location: target\Skytree-v3.2.0.jar
    for %%I in (target\Skytree-v3.2.0.jar) do echo JAR size: %%~zI bytes
    echo.
    echo Ready to deploy to Paper server!
    echo.
) else (
    echo JAR creation failed!
    exit /b 1
)

endlocal
