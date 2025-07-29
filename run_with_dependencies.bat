@echo off
setlocal

set "JAVA_HOME=%~dp0jdk-11"
set "JAVAC_CMD=javac"
if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVAC_CMD=%JAVA_HOME%\bin\javac"
)

echo Starting Java Vehicle Rental System with dependencies...
echo Compiling Java files...

REM Use --release 8 to target Java 8 and handle bootstrap classes automatically
%JAVAC_CMD% --release 8 -cp "lib/*;src" -d . src/*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b
)
echo Compilation successful!

echo Starting application...
java -cp "lib/*;." Main
if %errorlevel% neq 0 (
    pause
) 