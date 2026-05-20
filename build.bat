@echo off
echo Building Skytree Plugin...
echo.

REM Create output directory
if not exist "target\classes" mkdir "target\classes"

echo Downloading Paper API...
if not exist "libs" mkdir "libs"

REM Download Paper API if not exists
if not exist "libs\paper-api-1.21-R0.1-SNAPSHOT.jar" (
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21-R0.1-SNAPSHOT/paper-api-1.21-R0.1-20240710.013912-161.jar' -OutFile 'libs\paper-api.jar'}"
)

echo Compiling Java files...
dir /s /B src\main\java\com\wiredid\skytree\*.java > sources.txt

javac -cp "libs\paper-api.jar" -d target\classes -encoding UTF-8 -source 21 -target 21 @sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Copying resources...
xcopy /Y /E src\main\resources\* target\classes\

echo Creating JAR...
cd target\classes
jar cvf ..\Skytree-v3.2.0.jar *
cd ..\..

echo.
echo Build complete! JAR file: target\Skytree-v3.2.0.jar
echo.
pause
