@echo off
setlocal enabledelayedexpansion

if not "%JAVA_HOME%"=="" (
    set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
) else (
    where java >nul 2>nul
    if errorlevel 1 (
        echo JAVA_HOME is not set and java is not on PATH.
        exit /b 1
    )
    set JAVA_CMD=java
)

set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin

if not "%MAVEN_OPTS%"=="" set MVNW_OPTS=%MAVEN_OPTS%

%JAVA_CMD% %MVNW_OPTS% ^
  -classpath ".mvn\wrapper\maven-wrapper.jar" ^
  "-Dmaven.multiModuleProjectDirectory=%CD%" ^
  "-Dmaven.home=%MAVEN_HOME%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*
