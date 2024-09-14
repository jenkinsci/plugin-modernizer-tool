@echo off
REM This script copies the Maven local repository to the current directory.

echo Determining Maven local repository path...

REM Get the Maven local repository path using Maven's help:evaluate goal.
for /f "delims=" %%i in ('mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout') do set MAVEN_REPO=%%i

REM Check if the Maven repository path was found.
if "%MAVEN_REPO%"=="" (
    echo Failed to determine Maven local repository path.
    exit /b 1
)

echo Maven local repository path determined: %MAVEN_REPO%

REM Copy the Maven repository to the current directory.
echo Copying Maven repository to the current directory...
xcopy /E /I "%MAVEN_REPO%" ".m2"

REM Verify the copy operation.
if %errorlevel%==0 (
    echo Maven repository copied successfully to .m2
) else (
    echo Failed to copy Maven repository.
    exit /b 1
)
