@echo off
echo =================================
echo Skytree JAR Packager
echo =================================
echo.

REM Check if classes exist
if not exist "target\classes\com" (
    echo ERROR: No compiled classes found!
    echo Please compile the project first.
    echo.
    pause
    exit /b 1
)

echo Creating JAR from existing classes...

REM Copy plugin.yml and config.yml if not already there
copy /Y "src\main\resources\plugin.yml" "target\classes\" >nul 2>&1
copy /Y "src\main\resources\config.yml" "target\classes\" >nul 2>&1

REM Create JAR
cd "target\classes"
jar cvfm "..\Skytree-v3.2.0.jar" "..\..\MANIFEST.MF" * >nul 2>&1
if not exist "..\Skytree-v3.2.0.jar" (
    jar cvf "..\Skytree-v3.2.0.jar" * >nul 2>&1
)
cd "..\..."

if exist "target\Skytree-v3.2.0.jar" (
    echo.
    echo =================================
    echo SUCCESS!
    echo =================================
    for %%I in ("target\Skytree-v3.2.0.jar") do echo Size: %%~zI bytes
    echo Location: target\Skytree-v3.2.0.jar
    echo.
    echo IMPORTANT: This JAR needs Paper API to run!
    echo Place in plugins\ folder of PaperMC 1.21 server
    echo.
) else (
    echo ERROR: JAR creation failed
    echo.
)

pause
