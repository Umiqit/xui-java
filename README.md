# XUI VPN Bot

Telegram-бот для управления VPN-ключами через панель **XUI (3X-UI)**.

## Возможности

- Просмотр профиля и баланса (Telegram Stars ⭐)
- Управление VPN-ключами (список, статистика трафика, сброс, удаление)
- Пополнение баланса через Telegram Stars
- Админ-панель:   добавление ключей пользователям, просмотр inbound'ов

## Стек

- Java 17
- Maven
- SQLite / PostgreSQL + HikariCP
- Telegram Bots API (long-polling)
- OkHttp + Jackson
- Docker + Docker Compose
- Nginx Proxy Manager

## Быстрая установка (рекомендуется)

Требуется чистый сервер на **Ubuntu/Debian** с root-доступом.

```bash
git clone https://github.com/YOUR_REPO/xui-java.git
cd xui-java
sudo ./install.sh install
```

Скрипт автоматически установит Docker, Docker Compose, скопирует проект в `/opt/xui-bot` и создаст `.env`.

**Обязательно отредактируйте `.env`:**

```bash
sudo nano /opt/xui-bot/.env
```

Заполните:
- `BOT_TOKEN` — токен от @BotFather
- `BOT_USERNAME` — имя бота
- `ADMIN_IDS` — ID админов через запятую
- `XUI_URL`, `XUI_USERNAME`, `XUI_PASSWORD` — данные от панели XUI

Запуск:
```bash
sudo /opt/xui-bot/install.sh start
```

## Управление через скрипт

```bash
sudo /opt/xui-bot/install.sh start      # Запуск
sudo /opt/xui-bot/install.sh stop       # Остановка
sudo /opt/xui-bot/install.sh restart    # Перезапуск
sudo /opt/xui-bot/install.sh status     # Статус контейнеров
sudo /opt/xui-bot/install.sh logs       # Логи бота
sudo /opt/xui-bot/install.sh logs npm   # Логи Nginx Proxy Manager
sudo /opt/xui-bot/install.sh update     # Обновление из Git + пересборка
sudo /opt/xui-bot/install.sh uninstall  # Полное удаление со всеми данными
```

## Nginx Proxy Manager

После запуска откройте в браузере:
```
http://YOUR_SERVER_IP:81
```
- **Логин:** `admin@example.com`
- **Пароль:** `changeme`

Через NPM можно выпустить SSL-сертификаты и направить домен на XUI-панель или другие сервисы.

## Docker Compose (вручную)

Если хотите управлять вручную без скрипта:

```bash
cd /opt/xui-bot
docker compose up -d --build
docker compose logs -f bot
```

## Структура сервисов

| Сервис | Описание | Порты |
|--------|----------|-------|
| `bot` | Сам Telegram-бот | — |
| `db` | PostgreSQL (данные бота) | — (внутри сети) |
| `npm` | Nginx Proxy Manager | 80, 443, 81 |

Персистентные данные хранятся в `/opt/xui-bot/data/`.

## Локальная разработка (без Docker)

```bash
# SQLite по умолчанию
cp .env.example .env
mvn clean package
java -jar target/xui-bot-1.0-SNAPSHOT.jar
```

Для работы с SQLite оставьте `DB_TYPE=sqlite` (или не указывайте переменную).

## Тесты

```bash
mvn test
```

## Переменные окружения

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `BOT_TOKEN` | Токен Telegram бота | — |
| `BOT_USERNAME` | Юзернейм бота | — |
| `ADMIN_IDS` | ID админов через запятую | — |
| `XUI_URL` | URL панели XUI | — |
| `XUI_USERNAME` | Логин от панели | — |
| `XUI_PASSWORD` | Пароль от панели | — |
| `XUI_CERT_PATH` | Путь к self-signed серту (опц.) | — |
| `DB_TYPE` | Тип БД: `sqlite` или `postgres` | `sqlite` |
| `DB_PATH` | Путь к файлу SQLite (для sqlite) | `bot.db` |
| `DB_HOST` | Хост PostgreSQL | — |
| `DB_PORT` | Порт PostgreSQL | `5432` |
| `DB_NAME` | Имя базы PostgreSQL | — |
| `DB_USER` | Пользователь PostgreSQL | — |
| `DB_PASSWORD` | Пароль PostgreSQL | — |
