@echo off
setlocal

:: =====================================================
::  EgoGraph Android Dev - Initial Setup (Run Once)
:: =====================================================
:: ポートプロキシとファイアウォールの初期設定。
:: 初回のみ実行すれば良い（再起動後も設定は保持される）。
::
:: [重要] 管理者として実行すること！
:: =====================================================

:: --- System Path Definitions ---
set NETSH_CMD=%SystemRoot%\System32\netsh.exe
set POWERSHELL_CMD=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe

:: --- Port Configuration ---
:: 外部ポート5559を使用（5555はエミュレータが使うため競合回避）
set EXTERNAL_PORT=5559
set INTERNAL_PORT=5555

echo ===================================================
echo  EgoGraph Android Dev - Initial Setup
echo ===================================================
echo.
echo This script sets up port proxy and firewall rules.
echo Run this ONCE (settings persist across reboots).
echo.
echo Port Configuration:
echo   External: %EXTERNAL_PORT% (Linux connects here)
echo   Internal: %INTERNAL_PORT% (Emulator adbd)
echo.

:: =====================================================
:: Step 1: ポートプロキシ設定
:: =====================================================
echo [1/3] Setting up Port Proxy (%EXTERNAL_PORT% -^> %INTERNAL_PORT%)...

:: 既存ルール削除（冪等性確保）
"%NETSH_CMD%" interface portproxy delete v4tov4 listenport=%EXTERNAL_PORT% listenaddress=0.0.0.0 >nul 2>&1

:: 新規ルール追加
"%NETSH_CMD%" interface portproxy add v4tov4 listenport=%EXTERNAL_PORT% listenaddress=0.0.0.0 connectport=%INTERNAL_PORT% connectaddress=127.0.0.1

echo     - Done.

:: =====================================================
:: Step 2: ファイアウォール設定
:: =====================================================
echo.
echo [2/3] Configuring Firewall (Tailscale/Local only)...

:: ルールが存在すれば更新、なければ作成
"%POWERSHELL_CMD%" -Command "if (Get-NetFirewallRule -DisplayName 'ADB Bridge %EXTERNAL_PORT%' -ErrorAction SilentlyContinue) { Set-NetFirewallRule -DisplayName 'ADB Bridge %EXTERNAL_PORT%' -RemoteAddress 100.0.0.0/8,192.168.0.0/16,LocalSubnet } else { New-NetFirewallRule -DisplayName 'ADB Bridge %EXTERNAL_PORT%' -Direction Inbound -LocalPort %EXTERNAL_PORT% -Protocol TCP -Action Allow -RemoteAddress 100.0.0.0/8,192.168.0.0/16,LocalSubnet }"

echo     - Done.

:: =====================================================
:: Step 3: 設定確認
:: =====================================================
echo.
echo [3/3] Verifying Setup...
echo.
echo Port Proxy Configuration:
"%NETSH_CMD%" interface portproxy show v4tov4

echo.
echo Firewall Rule:
"%POWERSHELL_CMD%" -Command "Get-NetFirewallRule -DisplayName 'ADB Bridge %EXTERNAL_PORT%' | Select-Object DisplayName, Enabled, Direction, Action | Format-Table -AutoSize"

echo.
echo ===================================================
echo  INITIAL SETUP COMPLETE!
echo ===================================================
echo.
echo These settings persist across reboots.
echo You only need to run this script once.
echo.
echo [Next Step]
echo Run win_start_emulator.bat to start the emulator.
echo (No admin rights required for that script)
echo.
pause
