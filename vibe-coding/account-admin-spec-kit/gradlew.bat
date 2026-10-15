@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "GRADLE_VERSION=8.14.3"
if "%GRADLE_USER_HOME%"=="" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "DISTRIBUTION=%GRADLE_USER_HOME%\starter-distributions\gradle-%GRADLE_VERSION%"
if not exist "%DISTRIBUTION%\bin\gradle.bat" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $root=Join-Path $env:GRADLE_USER_HOME 'starter-distributions'; New-Item -ItemType Directory -Force $root | Out-Null; $zip=Join-Path $root ('gradle-'+$env:GRADLE_VERSION+'-bin.zip'); if (!(Test-Path $zip)) { Invoke-WebRequest -Uri ('https://services.gradle.org/distributions/gradle-'+$env:GRADLE_VERSION+'-bin.zip') -OutFile $zip }; Expand-Archive -Path $zip -DestinationPath $root -Force"
  if errorlevel 1 exit /b 1
)
call "%DISTRIBUTION%\bin\gradle.bat" -p "%SCRIPT_DIR%" %*
