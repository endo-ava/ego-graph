#!/bin/bash

# 接続テスト用スクリプト（ビルドなし）
# トラブルシューティング時に接続確認のみ行う

PORT=5559

# IPアドレスの取得（優先順位: 引数 > .env.local > 対話入力）
IP_ADDRESS=$1

if [ -z "$IP_ADDRESS" ]; then
    ENV_FILE="$(dirname "$0")/../../../../.env.local"
    if [ -f "$ENV_FILE" ]; then
        WINDOWS_IP=$(grep "^WINDOWS_IP=" "$ENV_FILE" | cut -d'=' -f2 | xargs)
        if [ -n "$WINDOWS_IP" ]; then
            IP_ADDRESS=$WINDOWS_IP
        fi
    fi
fi

if [ -z "$IP_ADDRESS" ]; then
    echo "Usage: $0 <WINDOWS_IP_ADDRESS>"
    echo -n "Enter Windows IP Address: "
    read IP_ADDRESS
fi

if [ -z "$IP_ADDRESS" ]; then
    echo "Error: IP Address is required."
    exit 1
fi

echo "========================================"
echo " ADB Connection Test"
echo "========================================"
echo ""

# 1. ネットワーク到達性
echo "[1/3] Network connectivity..."
if nc -zv $IP_ADDRESS $PORT 2>&1 | grep -q "succeeded\|open"; then
    echo "      -> OK (port $PORT reachable)"
else
    echo "      -> FAILED (port $PORT not reachable)"
    echo ""
    echo "Possible causes:"
    echo "  - Windows firewall blocking port $PORT"
    echo "  - Port proxy not configured (run win_initial_setup.bat)"
    echo "  - Emulator not running"
    exit 1
fi

# 2. ADB接続
echo ""
echo "[2/3] ADB connection..."
adb disconnect $IP_ADDRESS:$PORT > /dev/null 2>&1
OUTPUT=$(adb connect $IP_ADDRESS:$PORT 2>&1)

if echo "$OUTPUT" | grep -q "connected to\|already connected"; then
    echo "      -> OK (connected)"
else
    echo "      -> FAILED: $OUTPUT"
    exit 1
fi

# 3. デバイス状態
echo ""
echo "[3/3] Device state..."
DEVICE_STATE=$(adb -s $IP_ADDRESS:$PORT get-state 2>/dev/null)

case "$DEVICE_STATE" in
    "device")
        echo "      -> OK (device ready)"
        ;;
    "unauthorized")
        echo "      -> UNAUTHORIZED"
        echo ""
        echo "Please tap 'Allow' on the emulator screen."
        exit 1
        ;;
    "offline")
        echo "      -> OFFLINE"
        echo ""
        echo "See SKILL.md Phase 4 for troubleshooting."
        exit 1
        ;;
    *)
        echo "      -> UNKNOWN: $DEVICE_STATE"
        exit 1
        ;;
esac

echo ""
echo "========================================"
echo " SUCCESS: Connection Ready"
echo "========================================"
echo ""
echo "Device: $IP_ADDRESS:$PORT"
echo ""
echo "Next steps:"
echo "  - Debug: ./.claude/skills/android-adb-debug/scripts/linux_connect_and_install.sh"
echo "  - Manual: adb -s $IP_ADDRESS:$PORT shell"
