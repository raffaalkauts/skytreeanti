@echo off
set "MAVEN_HOME=%~dp0apache-maven-3.9.5"
set "PATH=%MAVEN_HOME%\bin;%PATH%"
echo Using MAVEN_HOME: %MAVEN_HOME%
call mvn clean package -DskipTests
