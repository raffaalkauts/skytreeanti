@echo off
echo ===================================
echo Skytree Plugin - Quick Build
echo ===================================
echo.

REM Create directories
if not exist "target\classes" mkdir "target\classes"
if not exist "libs" mkdir "libs"

REM Download Paper API
echo [1/4] Checking dependencies...
if not exist "libs\paper-api.jar" (
    echo Downloading Paper API...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21-R0.1-SNAPSHOT/paper-api-1.21-R0.1-20240710.013912-161.jar' -OutFile 'libs\paper-api.jar'" 2>nul
)

REM Compile
echo [2/4] Compiling Java sources...
del sources.txt 2>nul
powershell -Command "Get-ChildItem -Path 'src\main\java' -Recurse -Filter *.java | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' } | Out-File -Encoding ASCII sources.txt"

if exist "libs\paper-api.jar" (
    javac -cp "libs\paper-api.jar" -d "target\classes" -encoding UTF-8 @sources.txt 2>compile-errors.txt
) else (
    echo WARNING: Paper API not found, compiling without it...
    javac -d "target\classes" -encoding UTF-8 @sources.txt 2>compile-errors.txt
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo COMPILATION FAILED!
    echo.
    type compile-errors.txt
    echo.
    exit /b 1
)

echo Compilation successful!

REM Copy resources
echo [3/4] Copying resources...
if exist "src\main\resources\plugin.yml" copy /Y "src\main\resources\plugin.yml" "target\classes\" >nul
if exist "src\main\resources\config.yml" copy /Y "src\main\resources\config.yml" "target\classes\" >nul

REM Create JAR
echo [4/4] Creating JAR file...
cd "target\classes"
jar cvf "..\Skytree-v3.2.1.jar" * >nul 2>&1
cd "..\.."

if exist "target\Skytree-v3.2.1.jar" (
    echo.
    echo ===================================
    echo BUILD SUCCESS!
    echo ===================================
    for %%I in ("target\Skytree-v3.2.1.jar") do echo JAR Size: %%~zI bytes
    echo Location: target\Skytree-v3.2.1.jar
    echo.
) else (
    echo.
    echo BUILD FAILED - JAR not created
    echo.
)
