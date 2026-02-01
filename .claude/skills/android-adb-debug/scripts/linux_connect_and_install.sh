#!/bin/bash

# 使用法: ./.claude/skills/android-adb-debug/scripts/linux_connect_and_install.sh [Windows_IP]
# 例: ./.claude/skills/android-adb-debug/scripts/linux_connect_and_install.sh 100.x.x.x

# ポート5559を使用（Windows側でportproxy 5559→5555 を設定）
# エミュレータが5555を直接使えるよう、外部公開ポートを分離
PORT=5559

# IPアドレスの取得（優先順位: 引数 > .env.local > 対話入力）
IP_ADDRESS=$1

# .env.local から WINDOWS_IP を読み込み（引数がない場合のみ）
if [ -z "$IP_ADDRESS" ]; then
    ENV_FILE="$(dirname "$0")/../../../../.env.local"
    if [ -f "$ENV_FILE" ]; then
        # .env.local から WINDOWS_IP を抽出（コメント行と空行を無視）
        WINDOWS_IP=$(grep "^WINDOWS_IP=" "$ENV_FILE" | cut -d'=' -f2 | xargs)
        if [ -n "$WINDOWS_IP" ]; then
            IP_ADDRESS=$WINDOWS_IP
        fi
    fi
fi

# それでもIPアドレスがない場合は入力を求める
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
echo " 1. Connecting to ADB..."
echo "========================================"

# 既存の接続を切断
adb disconnect $IP_ADDRESS:$PORT > /dev/null 2>&1

# 接続ループ (認証待ち対応)
# 5秒 x 20回 = 最大100秒待機
MAX_RETRIES=20
COUNT=0

while [ $COUNT -lt $MAX_RETRIES ]; do
    OUTPUT=$(adb connect $IP_ADDRESS:$PORT 2>&1)

    # IPアドレスをマスクして表示
    MASKED_OUTPUT=$(echo "$OUTPUT" | sed "s/$IP_ADDRESS/[REDACTED]/g")

    if echo "$OUTPUT" | grep -q "connected to"; then
        echo " -> Connection established."
        break
    elif echo "$OUTPUT" | grep -q "failed to authenticate"; then
        ATTEMPT_NUM=$((COUNT+1))
        echo " [!] AUTHENTICATION REQUIRED (Attempt $ATTEMPT_NUM/$MAX_RETRIES)"
        echo "     Please check the Emulator screen on Windows."
        echo "     Tap 'Allow' on the 'Allow USB debugging?' popup."
        echo "     (Waiting 5 seconds...)"
        sleep 5
    elif echo "$OUTPUT" | grep -q "already connected"; then
        break
    else
        echo " -> Waiting for device... ($MASKED_OUTPUT)"
        sleep 3
    fi
    COUNT=$((COUNT+1))
done

# 最終確認
DEVICE_STATE=$(adb -s $IP_ADDRESS:$PORT get-state 2>/dev/null)

if [ "$DEVICE_STATE" == "device" ]; then
    echo "SUCCESS: Connected and Authenticated!"
elif [ "$DEVICE_STATE" == "unauthorized" ]; then
    echo "ERROR: Device is UNAUTHORIZED."
    echo "       You MUST click 'Allow' on the emulator screen."
    exit 1
else
    echo "ERROR: Failed to connect. State: $DEVICE_STATE"
    exit 1
fi

echo "========================================"
echo " 2. Building and Installing Debug APK..."
echo "========================================"

# frontendディレクトリへ移動 (gradlewがある場所)
# スクリプト位置: .claude/skills/android-adb-debug/scripts/
cd "$(dirname "$0")/../../../../frontend"

./gradlew :androidApp:installDebug

if [ $? -eq 0 ]; then
    echo "========================================"
    echo " SUCCESS! App installed."
    echo " Launching app..."
    echo "========================================"
    # アプリを起動
    adb -s $IP_ADDRESS:$PORT shell am start -n dev.egograph.app/.MainActivity
else
    echo "========================================"
    echo " BUILD FAILED."
    echo "========================================"
    exit 1
fi
