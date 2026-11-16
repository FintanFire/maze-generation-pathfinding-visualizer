@echo off
setlocal enabledelayedexpansion

set "JAVAC_CMD=javac"
set "JAVA_CMD=java"

where javac >nul 2>&1
if errorlevel 1 (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-*") do (
        if exist "%%D\bin\javac.exe" (
            set "JAVAC_CMD=%%D\bin\javac.exe"
            set "JAVA_CMD=%%D\bin\java.exe"
        )
    )
)

if not exist bin mkdir bin
"%JAVAC_CMD%" -encoding UTF-8 -d bin src/*.java
if errorlevel 1 (
    echo Compilation failed. Make sure a JDK ^(javac^) is installed.
    pause
    exit /b 1
)

"%JAVA_CMD%" -cp bin MazeVisualizerApp
