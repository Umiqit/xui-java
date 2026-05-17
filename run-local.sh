#!/bin/bash
# Локальный запуск бота в DEBUG-режиме (без Docker)
set -e

echo "[DEBUG] Building project..."
mvn clean package -DskipTests -q

echo "[DEBUG] Starting bot with DEBUG logs..."
export LOG_LEVEL=DEBUG
java -DLOG_LEVEL=DEBUG -jar target/xui-bot-1.0-SNAPSHOT.jar
