@echo off
setlocal

set "JAVA_HOME=%~dp0jdk-11"
set "JAVAC_CMD=javac"
if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVAC_CMD=%JAVA_HOME%\bin\javac"
)

echo Starting Java Vehicle Rental System with dependencies...
echo Compiling Java files...

:: Build a temporary list of all .java files under src (recursively)
if exist "%~dp0sources.txt" del "%~dp0sources.txt"
for /R "%~dp0src" %%f in (*.java) do @echo %%f>>"%~dp0sources.txt"

:: Check we found sources
if not exist "%~dp0sources.txt" (
    echo No Java source files found under src\
    pause
    exit /b 1
)

:: Compile using the sources list file
%JAVAC_CMD% --release 8 -cp "lib/*;src" -d . @"%~dp0sources.txt"
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b
)
del "%~dp0sources.txt"
echo Compilation successful!

echo Starting application...
java -cp "lib/*;." main.Main
if %errorlevel% neq 0 (
    pause
) 