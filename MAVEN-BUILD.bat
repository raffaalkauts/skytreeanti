@echo off
echo ============================================
echo Maven Download and Build Script
echo ============================================
echo.

REM Download Maven 3.9.5 (working URL)
echo [1/4] Downloading Maven...
powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.zip' -OutFile 'maven.zip'"

if not exist maven.zip (
    echo Maven download failed!
    echo Try manual download from: https://maven.apache.org/download.cgi
    exit /b 1
)

echo [2/4] Extracting Maven...
powershell -Command "Expand-Archive -Path maven.zip -DestinationPath . -Force"

echo [3/4] Building project...
echo This will take 1-2 minutes...
echo.

REM Set Maven path and build
set "MAVEN_HOME=%CD%\apache-maven-3.9.5"
set "PATH=%MAVEN_HOME%\bin;%PATH%"

REM Build with Maven
call apache-maven-3.9.5\bin\mvn clean package -DskipTests

if exist target\Skytree-v3.5.0-RELEASE.jar (
    echo.
    echo ============================================
    echo BUILD SUCCESS!
    echo ============================================
    echo.
    echo JAR Location: target\Skytree-v3.5.0-RELEASE.jar
    for %%I in (target\Skytree-v3.5.0-RELEASE.jar) do echo JAR Size: %%~zI bytes
    echo.
    echo Ready to deploy to Paper server!
    echo.
    
    REM Cleanup
    echo [4/4] Cleaning up...
    del maven.zip
    
) else (
    echo.
    echo Build failed! Check errors above.
    exit /b 1
)
