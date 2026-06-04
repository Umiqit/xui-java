# XUI VPN Bot + Site

Telegram-бот и веб-сайт для управления VPN-ключами через панель **XUI (3X-UI)**.

## Возможности

### Telegram-бот
- Просмотр профиля и баланса (Telegram Stars ⭐)
- Управление VPN-ключами (список, статистика трафика, сброс, удаление)
- Пополнение баланса через Telegram Stars
- Админ-панель в боте: добавление ключей пользователям, просмотр inbound'ов

### Веб-сайт (Личный кабинет)
- Авторизация через Telegram Login Widget
- Просмотр ключей и баланса в браузере
- Админ-панель с управлением пользователями, ключами и платежами
- Удобный интерфейс на Thymeleaf + Material Design 3

## Стек

- **Java 17**, Maven
- **Spring Boot 3** + Thymeleaf + Spring Data JPA (сайт)
- **PostgreSQL** / SQLite + HikariCP (бот)
- **Telegram Bots API** (long-polling)
- **OkHttp** + Jackson
- **Docker** + Docker Compose
- **Nginx Proxy Manager** (SSL + проксирование)

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

### Глобальные команды
```bash
sudo /opt/xui-bot/install.sh start      # Запуск всего
sudo /opt/xui-bot/install.sh stop       # Остановка всего
sudo /opt/xui-bot/install.sh restart    # Перезапуск всего
sudo /opt/xui-bot/install.sh status     # Статус контейнеров
sudo /opt/xui-bot/install.sh logs       # Логи бота
sudo /opt/xui-bot/install.sh update     # Обновление из Git + пересборка
sudo /opt/xui-bot/install.sh uninstall  # Полное удаление со всеми данными
```

### Только сайт
```bash
sudo /opt/xui-bot/install.sh site-start     # Запуск сайта
sudo /opt/xui-bot/install.sh site-stop      # Остановка сайта
sudo /opt/xui-bot/install.sh site-restart   # Перезапуск сайта
sudo /opt/xui-bot/install.sh site-logs      # Логи сайта
sudo /opt/xui-bot/install.sh site-update    # Пересборка сайта
```

### Только бот
```bash
sudo /opt/xui-bot/install.sh bot-start      # Запуск бота
sudo /opt/xui-bot/install.sh bot-stop       # Остановка бота
sudo /opt/xui-bot/install.sh bot-restart    # Перезапуск бота
sudo /opt/xui-bot/install.sh bot-logs       # Логи бота
sudo /opt/xui-bot/install.sh bot-update     # Пересборка бота
```

### База данных
```bash
sudo /opt/xui-bot/install.sh db-logs        # Логи PostgreSQL
```

## Настройка домена и SSL (Nginx Proxy Manager)

После запуска откройте NPM:
```
http://YOUR_SERVER_IP:81
```
- **Логин:** `admin@example.com`
- **Пароль:** `changeme`

1. Создайте Proxy Host: укажите ваш домен (например, `dreamchatai.website`) и бэкенд `http://xui-bot-site:8080`
2. Включите SSL через Let's Encrypt
3. **Важно:** домен должен быть добавлен в настройках бота через @BotFather (Bot Settings → Domain) для работы Telegram Login Widget

## Docker Compose (вручную)

Если хотите управлять вручную без скрипта:

```bash
cd /opt/xui-bot
docker compose up -d --build
docker compose logs -f bot
docker compose logs -f site
```

## Структура сервисов

| Сервис | Описание | Порты |
|--------|----------|-------|
| `bot` | Telegram-бот | — |
| `site` | Веб-приложение (личный кабинет) | 8080 (внутри сети) |
| `db` | PostgreSQL (данные бота и сайта) | — (внутри сети) |
| `npm` | Nginx Proxy Manager | 80, 443, 81 |
| `portainer` | Управление контейнерами | 9000 |

Персистентные данные хранятся в `/opt/xui-bot/data/`.

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
| `ADMIN_PANEL_PATH` | Путь к админке сайта | `/sys/dc-panel` |
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
