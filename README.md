# XUI VPN Bot + Site

Telegram-бот и веб-сайт для управления VPN-ключами через панель **XUI (3X-UI)**.

## Возможности

### Telegram-бот
- **Магазин VPN-тарифов** — покупка ключей за баланс в Telegram Stars ⭐
- Просмотр профиля и баланса (Telegram Stars ⭐)
- Управление VPN-ключами (список, статистика трафика, сброс, удаление)
- Пополнение баланса через Telegram Stars
- Поддержка и отзывы
- Админ-панель в боте: добавление ключей пользователям, просмотр inbound'ов

### Веб-сайт (Личный кабинет)
- Авторизация через Telegram Login Widget
- Просмотр ключей и баланса в браузере
- История пополнений и заказов
- Админ-панель с управлением пользователями, ключами и платежами
- FAQ и гарантии через встроенный сайт
- Удобный интерфейс на Thymeleaf + Material Design 3

## Стек

- **Java 17**, Maven
- **Spring Boot 3** + Thymeleaf + Spring Data JPA (сайт)
- **PostgreSQL** / SQLite + HikariCP
- **Telegram Bots API** (long-polling)
- **OkHttp** + Jackson
- **Docker** + Docker Compose
- **Nginx Proxy Manager** (SSL + проксирование)
- Встроенный веб-сервер (Spring Boot site) для FAQ и гарантий

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
- `SITE_URL` — URL сайта для ссылок на FAQ и гарантии (опционально)

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

##3 Настройка домена и SSL (Nginx Proxy Manager)

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

## Структура проекта

```
xui-java/
├── bot/          # Telegram-бот (Java + Maven)
│   ├── Dockerfile
│   ├── pom.xml
│   ├── src/
│   └── run-local.sh
├── site/         # Spring Boot сайт
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── docker-compose.yml
├── install.sh
└── .env
```

## Локальная разработка (без Docker)

```bash
# Бот
cd bot
cp ../.env.example .env
mvn clean package
java -jar target/xui-bot-1.0-SNAPSHOT.jar

# Сайт
cd site
mvn clean package
java -jar target/site-1.0-SNAPSHOT.jar
```

Для работы с SQLite оставьте `DB_TYPE=sqlite` (или не указывайте переменную).

## Тесты

```bash
# Бот
cd bot && mvn test

# Сайт
cd site && mvn test
```

## Мультисерверность

Бот и сайт поддерживают управление **несколькими панелями x-ui** одновременно.

- Серверы хранятся в БД (таблица `servers`)
- При покупке ключа автоматически выбирается наименее загруженный активный сервер
- Админ может добавлять/удалять серверы через бота или сайт

**Telegram-команды админа:**
- `/servers` — список серверов со статусом
- `/add_server` — добавить новый x-ui сервер
- `/del_server` — удалить сервер
- `/xui_inbounds` — просмотр inbound'ов с выбором сервера
- `/add_key` — добавить ключ пользователю с выбором сервера

**На сайте:** админ-панель → раздел "Серверы" с формой добавления.

`XUI_URL` из `.env` используется как дефолтный сервер при первом запуске (если таблица серверов пуста). После этого сервера управляются через БД.

## Магазин и тарифы

Бот содержит встроенный магазин VPN-тарифов. Пользователь выбирает тариф, подтверждает покупку, и ключ автоматически создаётся на XUI-панели. Стоимость списывается с баланса в Telegram Stars.

**Тарифы по умолчанию** (создаются автоматически при первом запуске):

| Тариф | Срок | Трафик | Цена |
|-------|------|--------|------|
| 1 месяц | 30 дней | 100 GB | 150 |
| 3 месяца | 90 дней | 300 GB | 400 |
| 6 месяцев | 180 дней | безлимит | 700 |
| 12 месяцев | 365 дней | безлимит | 1200 |

> Тарифы хранятся в таблице `products` — при необходимости их можно изменить напрямую в БД.

## Личный кабинет

В разделе **Кабинет** пользователь может:
- Просматривать баланс, количество пополнений и заказов
- Пополнять баланс через Telegram Stars
- Смотреть историю покупок
- Переход к управлению ключами

## FAQ и Гарантии

Если задана переменная `SITE_URL`, бот генерирует ссылки на статические страницы:
- `/faq.html` — ответы на частые вопросы
- `/warranty.html` — информация о гарантиях

Страницы размещены в модуле `site` и доступны после запуска соответствующего сервиса.

## Переменные окружения

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `BOT_TOKEN` | Токен Telegram бота | — |
| `BOT_USERNAME` | Юзернейм бота | — |
| `ADMIN_IDS` | ID админов через запятую | — |
| `ADMIN_PANEL_PATH` | Путь к админке сайта | `/sys/dc-panel` |
| `XUI_URL` | URL панели XUI (опц., для начального сервера) | — |
| `XUI_USERNAME` | Логин от панели | — |
| `XUI_PASSWORD` | Пароль от панели | — |
| `XUI_CERT_PATH` | Путь к self-signed серту (опц.) | — |
| `SITE_URL` | URL сайта для FAQ / гарантий (опц.) | — |
| `DB_TYPE` | Тип БД: `sqlite` или `postgres` | `sqlite` |
| `DB_PATH` | Путь к файлу SQLite (для sqlite) | `bot.db` |
| `DB_HOST` | Хост PostgreSQL | — |
| `DB_PORT` | Порт PostgreSQL | `5432` |
| `DB_NAME` | Имя базы PostgreSQL | — |
| `DB_USER` | Пользователь PostgreSQL | — |
| `DB_PASSWORD` | Пароль PostgreSQL | — |
