@echo off
setlocal

:: =====================================================
::  EgoGraph Android Dev - Start Emulator
:: =====================================================
:: エミュレータを起動するスクリプト（毎回実行）。
:: 管理者権限は不要。
::
:: [前提] win_initial_setup.bat を一度実行済みであること。
:: =====================================================

:: --- User Settings ---
set AVD_NAME=Pixel_6
set EMULATOR_PATH=%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe
set ADB_PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
set TASKKILL_CMD=%SystemRoot%\System32\taskkill.exe
set TIMEOUT_CMD=%SystemRoot%\System32\timeout.exe

echo ===================================================
echo  EgoGraph Android Dev - Start Emulator
echo ===================================================
echo.
echo Target AVD: %AVD_NAME%
echo.

:: =====================================================
:: Step 1: 既存プロセス停止
:: =====================================================
echo [1/3] Killing existing emulator processes...
"%TASKKILL_CMD%" /F /IM emulator.exe >nul 2>&1
"%TASKKILL_CMD%" /F /IM qemu-system-x86_64.exe >nul 2>&1
echo     - Done.

:: =====================================================
:: Step 2: エミュレータ起動
:: =====================================================
echo.
echo [2/3] Starting Emulator on PORT 5554...
start "" "%EMULATOR_PATH%" -avd %AVD_NAME% -port 5554 -no-snapshot -gpu swiftshader_indirect
echo     - Emulator process started.

:: =====================================================
:: Step 3: 起動待機
:: =====================================================
echo.
echo [3/3] Waiting for emulator to boot (max 120 seconds)...

:: ADBサーバ起動
"%ADB_PATH%" start-server >nul 2>&1

set MAX_WAIT=24
set COUNT=0

:wait_loop
if %COUNT% geq %MAX_WAIT% goto wait_timeout

"%ADB_PATH%" devices 2>nul | findstr /C:"emulator-5554" | findstr /C:"device" >nul
if %ERRORLEVEL%==0 goto wait_success

echo     - Waiting... (%COUNT%/%MAX_WAIT%)
"%TIMEOUT_CMD%" /t 5 /nobreak >nul
set /a COUNT+=1
goto wait_loop

:wait_timeout
echo.
echo [WARNING] Emulator did not become ready within 120 seconds.
echo           It may still be booting. Check the emulator window.
goto show_status

:wait_success
echo     - Emulator is ready!

:show_status
echo.
echo ===================================================
echo  EMULATOR STARTED
echo ===================================================
echo.
echo ADB Devices:
"%ADB_PATH%" devices
echo.
echo ===================================================
echo  NEXT STEP (on Linux):
echo ===================================================
echo.
echo   adb connect ^<WINDOWS_IP^>:5559
echo   adb devices
echo.
echo Or run the script:
echo   ./.claude/skills/adb-connection-troubleshoot/scripts/linux_connect_and_install.sh
echo.
pause
