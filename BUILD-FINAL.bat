@echo off
echo ============================================
echo Skytree v3.2.0 - ULTIMATE BUILD SCRIPT
echo ============================================
echo All 100%% features included!
echo.

REM Check if Maven exists
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Maven detected! Building with Maven...
    echo.
    call mvn clean package -DskipTests
    
    if exist "target\Skytree-v3.2.0.jar" (
        echo.
        echo ============================================
        echo BUILD SUCCESS WITH MAVEN!
        echo ============================================
        echo JAR: target\Skytree-v3.2.0.jar
        for %%I in (target\Skytree-v3.2.0.jar) do echo Size: %%~zI bytes
        echo.
        echo Ready to deploy!
        exit /b 0
    ) else (
        echo Maven build failed, trying manual...
    )
)

echo.
echo Maven not found or failed, trying manual packaging...
echo.

REM Create directories
if not exist "manual-build" mkdir "manual-build"
cd manual-build

REM Copy all class files if they exist
if exist "..\target\classes" (
    xcopy /E /I /Y "..\target\classes\*" "." >nul
) else (
    echo ERROR: No compiled classes found!
    echo Please compile with an IDE first, then run this script.
    cd ..
    exit /b 1
)

REM Copy resources
if exist "..\src\main\resources\plugin.yml" copy /Y "..\src\main\resources\plugin.yml" "." >nul
if exist "..\src\main\resources\config.yml" copy /Y "..\src\main\resources\config.yml" "." >nul
if exist "..\src\main\resources\shop.yml" copy /Y "..\src\main\resources\shop.yml" "." >nul

REM Create JAR
jar cf ..\Skytree-v3.2.0-MANUAL.jar *

cd ..

if exist "Skytree-v3.2.0-MANUAL.jar" (
    echo.
    echo ============================================
    echo MANUAL BUILD SUCCESS!
    echo ============================================
    echo JAR: Skytree-v3.2.0-MANUAL.jar  
    for %%I in (Skytree-v3.2.0-MANUAL.jar) do echo Size: %%~zI bytes
    echo.
    echo NOTE: This JAR created from existing compiled classes
    echo.
) else (
    echo.
    echo ============================================  
    echo BUILD GUIDE
    echo ============================================
    echo.
    echo Please build using one of these methods:
    echo.
    echo 1. IntelliJ IDEA:
    echo    - Build ^> Build Project
    echo    - Then Build ^> Build Artifacts
    echo.
    echo 2. Eclipse:
    echo    - Project ^> Build All
    echo    - File ^> Export ^> JAR file
    echo.
    echo 3. VS Code:
    echo    - Install Maven extension
    echo    - Run: mvn clean package
    echo.
)

REM Cleanup
if exist "manual-build" rmdir /S /Q "manual-build"
