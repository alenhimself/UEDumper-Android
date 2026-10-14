@echo off
setlocal enabledelayedexpansion
REM MobileDumper-7 GUI — one-shot Windows build.
REM 1) Builds native executables (all Android ABIs) with the local NDK.
REM 2) Stages them into android-app assets.
REM 3) Builds the debug APK with the local Gradle + SDK in D:\Compile.

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"

set "JAVA_HOME=D:\Compile\jdk-21.0.12.1+1"
set "ANDROID_HOME=D:\Compile"
set "ANDROID_SDK_ROOT=D:\Compile"
set "ANDROID_NDK_HOME=D:\Compile\ndk\android-ndk-r27c"
set "GRADLE_USER_HOME=D:\Compile\gradle-home"

set "PATH=D:\Compile\gradle-8.7\bin;D:\Compile\jdk-21.0.12.1+1\bin;D:\Compile\platform-tools;D:\Compile\build-tools\34.0.0;%PATH%"
where cmake >nul 2>nul || ( echo [error] cmake not found on PATH & exit /b 1 )
where ninja >nul 2>nul || ( echo [error] ninja not found on PATH & exit /b 1 )
if not exist "%ANDROID_NDK_HOME%\build\cmake\android.toolchain.cmake" (
  echo [error] NDK not found at %ANDROID_NDK_HOME%
  exit /b 1
)

echo ==^> Building native executables (release: arm64, arm32, x86_64, x86)
for %%P in (android-arm64-exe android-armeabi-v7a-exe android-x86_64-exe android-x86-exe) do (
  echo ---- %%P
  cmake --preset %%P || exit /b 1
  cmake --build --preset %%P || exit /b 1
)

echo ==^> Staging binaries into app assets
set "ASSETS=%ROOT%\android-app\app\src\main\assets\dumper"
call :stage "%ROOT%\builds\android\exe\release\arm64\MobileDumper-7"      "%ASSETS%\arm64-v8a\MobileDumper-7" || exit /b 1
call :stage "%ROOT%\builds\android\exe\release\arm32\MobileDumper-7"      "%ASSETS%\armeabi-v7a\MobileDumper-7" || exit /b 1
call :stage "%ROOT%\builds\android\exe\release\x86_64\MobileDumper-7"     "%ASSETS%\x86_64\MobileDumper-7" || exit /b 1
call :stage "%ROOT%\builds\android\exe\release\x86\MobileDumper-7"        "%ASSETS%\x86\MobileDumper-7" || exit /b 1

echo ==^> Building debug APK
pushd "%ROOT%\android-app"
call gradle :app:assembleDebug --console=plain || ( popd & exit /b 1 )
popd

set "APK=%ROOT%\android-app\app\build\outputs\apk\debug\app-debug.apk"
if not exist "%APK%" ( echo [error] APK not produced & exit /b 1 )
copy /y "%APK%" "D:\Compile\MobileDumper7-v1.2.1-debug.apk" >nul
echo [ok] APK: %APK%
echo [ok] Copy: D:\Compile\MobileDumper7-v1.2.1-debug.apk
exit /b 0

:stage
if not exist "%~1" ( echo [error] missing binary: %~1 & exit /b 1 )
if not exist "%~dp2" mkdir "%~dp2"
copy /y "%~1" "%~2" >nul
echo   staged %~2
exit /b 0
