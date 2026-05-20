@echo off
echo ========================================
echo Skytree Custom Build Script
echo ========================================

if not exist target\classes mkdir target\classes
if not exist libs mkdir libs

REM Download dependencies if not present
if not exist "libs\paper-server.jar" (
    echo Downloading dependencies...
    powershell -ExecutionPolicy Bypass -File download_libs.ps1
)

echo.
echo Compiling Java sources...
echo.

REM Create sources list
powershell -Command "Get-ChildItem -Recurse -Filter *.java src\main\java | Resolve-Path -Relative | Out-File sources.txt -Encoding ASCII"

REM Compile using the server jar and libraries
javac -cp "libs\*;libs\paper-server.jar" -d "target\classes" -encoding UTF-8 -source 21 -target 21 -proc:none @sources.txt 2>build_error.txt

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo COMPILATION FAILED!
    type build_error.txt
    exit /b 1
)

echo Compilation successful!
echo.
echo Copying resources...
copy /Y "src\main\resources\plugin.yml" "target\classes\" >nul
copy /Y "src\main\resources\config.yml" "target\classes\" 2>nul
copy /Y "src\main\resources\shop.yml" "target\classes\" 2>nul
copy /Y "src\main\resources\kits.yml" "target\classes\" 2>nul
copy /Y "src\main\resources\mythic_items.json" "target\classes\" 2>nul
copy /Y "src\main\resources\quests.yml" "target\classes\" 2>nul
copy /Y "src\main\resources\shard_shop.yml" "target\classes\" 2>nul
copy /Y "src\main\resources\tab.yml" "target\classes\" 2>nul

echo Creating JAR...
cd target\classes
jar cf ..\skytree-3.2.3.jar *
cd ..\..

echo.
echo BUILD SUCCESS!
echo JAR: target\skytree-3.2.3.jar
