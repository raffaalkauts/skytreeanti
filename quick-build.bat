@echo off
echo ===================================
echo Skytree Plugin - Quick Build
echo ===================================
echo.

REM Create directories
if not exist "target\classes" mkdir "target\classes"
if not exist "libs" mkdir "libs"

REM Download Paper API (compact version)
echo [1/4] Downloading dependencies...
if not exist "libs\paper-api.jar" (
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21-R0.1-SNAPSHOT/paper-api-1.21-R0.1-20240710.013912-161.jar' -OutFile 'libs\paper-api.jar'" 2>nul
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to download Paper API. Building without it...
    )
)

REM Compile
echo [2/4] Compiling Java sources...
dir /s /B "src\main\java\com\wiredid\skytree\*.java" > sources.txt 2>nul

if exist "libs\paper-api.jar" (
    javac -cp "libs\paper-api.jar" -d "target\classes" -encoding UTF-8 @sources.txt 2>compile-errors.txt
) else (
    javac -d "target\classes" -encoding UTF-8 @sources.txt 2>compile-errors.txt
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo COMPILATION FAILED!
    echo Check compile-errors.txt for details
    type compile-errors.txt
    exit /b 1
)

REM Copy resources
echo [3/4] Copying resources...
xcopy /Y /Q "src\main\resources\*" "target\classes\" >nul 2>&1

REM Create JAR
echo [4/4] Creating JAR file...
cd target\classes
jar cvf "..\Skytree-v3.2.0.jar" * >nul 2>&1
cd ..\..

echo.
echo ===================================
echo BUILD SUCCESS!
echo ===================================
echo JAR Location: target\Skytree-v3.2.0.jar
echo.

dir target\Skytree-v3.2.0.jar | find "Skytree"
