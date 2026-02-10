@echo off
setlocal

echo ==========================================
echo      RoboFace CLI Builder ^& Runner
echo ==========================================

REM --- 1. FIND ANDROID SDK ---
if defined ANDROID_HOME (
    set "SDK_PATH=%ANDROID_HOME%"
    echo Found ANDROID_HOME: %ANDROID_HOME%
) else if defined ANDROID_SDK_ROOT (
    set "SDK_PATH=%ANDROID_SDK_ROOT%"
    echo Found ANDROID_SDK_ROOT: %ANDROID_SDK_ROOT%
) else if exist "%LOCALAPPDATA%\Android\Sdk" (
    set "SDK_PATH=%LOCALAPPDATA%\Android\Sdk"
    echo Found SDK at default location.
) else (
    echo [ERROR] Android SDK not found.
    echo Please set ANDROID_HOME or ensure the SDK is at %%LOCALAPPDATA%%\Android\Sdk
    set /p "SDK_PATH=Or enter full path to Android SDK here: "
)

REM Create local.properties
echo sdk.dir=%SDK_PATH:\=\\%> local.properties
echo Created local.properties pointing to %SDK_PATH%

REM --- 2. FIND GRADLE ---
set "GRADLE_CMD=gradle"

where gradle >nul 2>nul
if %errorlevel% equ 0 (
    echo Gradle found in PATH.
) else (
    echo Gradle not found in PATH. Searching common locations...
    
    REM Search in Android Studio folder
    for /d %%D in ("C:\Program Files\Android\Android Studio\gradle\gradle-*") do (
        if exist "%%D\bin\gradle.bat" (
            set "GRADLE_CMD=%%D\bin\gradle.bat"
            goto FoundGradle
        )
    )
    
    REM Search in C:\Gradle
    if exist "C:\Gradle\bin\gradle.bat" (
        set "GRADLE_CMD=C:\Gradle\bin\gradle.bat"
        goto FoundGradle
    )

    echo [WARNING] Gradle not found automatically.
    set /p "GRADLE_CMD=Enter full path to gradle.bat (or press Enter to try system gradle anyway): "
    if "%GRADLE_CMD%"=="" set "GRADLE_CMD=gradle"
)

:FoundGradle
echo Using Gradle: "%GRADLE_CMD%"

REM --- 3. BUILD APK ---
echo.
echo Building Debug APK...
call "%GRADLE_CMD%" assembleDebug
if %errorlevel% neq 0 (
    echo [ERROR] Build failed.
    pause
    exit /b %errorlevel%
)

REM --- 4. INSTALL APK ---
echo.
echo Build Successful! Looking for device...
set "ADB_CMD=%SDK_PATH%\platform-tools\adb.exe"

if not exist "%ADB_CMD%" (
    echo ADB not found at %ADB_CMD%. Assuming 'adb' is in PATH.
    set "ADB_CMD=adb"
)

"%ADB_CMD%" devices
echo.
echo Attempting install...
"%ADB_CMD%" install -r app\build\outputs\apk\debug\app-debug.apk

if %errorlevel% neq 0 (
    echo [ERROR] Installation failed. Ensure device is connected and USB debugging is ON.
) else (
    echo [SUCCESS] Application installed!
)

pause
