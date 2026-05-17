#!/bin/bash
set -e

JAR="xui-bot-1.0-SNAPSHOT.jar"
DEPLOY_DIR="/opt/xui_bot"

mvn clean package -q

mkdir -p "$DEPLOY_DIR"
cp "target/$JAR" "$DEPLOY_DIR/"
cp .env "$DEPLOY_DIR/" 2>/dev/null || true
cp xui_bot.service /etc/systemd/system/

systemctl daemon-reload
systemctl enable xui_bot
systemctl restart xui_bot
echo "Done. Status:"
systemctl status xui_bot --no-pager
