@echo off
setlocal enabledelayedexpansion
echo ===================================
echo Skytree Quick Build
echo ===================================
echo.

REM Create output directory
if not exist target\classes mkdir target\classes
if not exist libs mkdir libs

REM Check for dependencies
if exist "libs\paper-server.jar" (
    set "CLASSPATH=libs\paper-server.jar"
    echo Using local paper-server.jar
) else (
    echo [1/4] Downloading Paper API...
    if not exist "libs\paper-api.jar" (
        powershell -Command "try { Invoke-WebRequest -Uri 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21-R0.1-SNAPSHOT/paper-api-1.21-R0.1-20240710.013912-161.jar' -OutFile 'libs\paper-api.jar' } catch { exit 1 }"
    )
    set "CLASSPATH=libs\paper-api.jar"
)

REM Compile all files at once
echo [2/4] Compiling...
dir /s /B "src\main\java\*.java" > sources.txt
javac -cp "%CLASSPATH%" -d "target\classes" -sourcepath "src\main\java" -encoding UTF-8 @sources.txt 2>compile.log

if %ERRORLEVEL% NEQ 0 (
    set ERROR=1
) else (
    set ERROR=0
)

if !ERROR! EQU 1 (
    echo COMPILATION FAILED!
    type compile.log
    del sources.txt
    exit /b 1
)

echo - Compiled successfully
del sources.txt

REM Copy resources
echo [3/4] Copying resources...
copy /Y "src\main\resources\plugin.yml" "target\classes\" >nul 2>&1
copy /Y "src\main\resources\config.yml" "target\classes\" >nul 2>&1
if exist "src\main\resources\mythic_items.json" copy /Y "src\main\resources\mythic_items.json" "target\classes\" >nul 2>&1

REM Create JAR
echo [4/4] Creating JAR...
cd target\classes
jar cf ..\Skytree-v3.2.0.jar *
cd ..\..

if exist "target\Skytree-v3.2.0.jar" (
    echo.
    echo ===================================
    echo SUCCESS!
    echo ===================================
    echo JAR: target\Skytree-v3.2.0.jar
    for %%I in (target\Skytree-v3.2.0.jar) do echo Size: %%~zI bytes
    echo.
) else (
    echo FAILED - JAR not created
    exit /b 1
)

endlocal
