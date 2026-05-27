#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="/opt/xui-bot"
PROJECT_NAME="xui-bot"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERROR]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }

check_root() {
    if [ "$EUID" -ne 0 ]; then
        err "Please run as root or with sudo"
        exit 1
    fi
}

install_docker() {
    if command -v docker &> /dev/null && (docker compose version &> /dev/null || command -v docker-compose &> /dev/null); then
        ok "Docker and Docker Compose already installed"
        return
    fi

    info "Installing Docker and Docker Compose..."
    apt-get update -qq
    apt-get install -y -qq curl ca-certificates gnupg lsb-release software-properties-common

    if ! command -v docker &> /dev/null; then
        info "Installing Docker..."
        curl -fsSL https://get.docker.com | sh
        systemctl enable docker >/dev/null 2>&1
        systemctl start docker
    fi

    if ! docker compose version &> /dev/null && ! command -v docker-compose &> /dev/null; then
        info "Installing Docker Compose plugin..."
        apt-get install -y -qq docker-compose-plugin
    fi

    ok "Docker installed"
}

prompt() {
    local prompt_text="$1"
    local default_value="$2"
    local is_required="$3"
    local value

    while true; do
        if [ -n "$default_value" ]; then
            read -rp "$prompt_text [$default_value]: " value
        else
            read -rp "$prompt_text: " value
        fi

        if [ -z "$value" ] && [ -n "$default_value" ]; then
            value="$default_value"
        fi

        if [ "$is_required" = "required" ] && [ -z "$value" ]; then
            warn "This field is required. Please enter a value."
            continue
        fi

        printf '%s' "$value"
        return
    done
}

prompt_secret() {
    local prompt_text="$1"
    local default_value="$2"
    local is_required="$3"
    local value

    while true; do
        if [ -n "$default_value" ]; then
            read -rsp "$prompt_text [$default_value]: " value
        else
            read -rsp "$prompt_text: " value
        fi
        echo >&2

        if [ -z "$value" ] && [ -n "$default_value" ]; then
            value="$default_value"
        fi

        if [ "$is_required" = "required" ] && [ -z "$value" ]; then
            warn "This field is required. Please enter a value." >&2
            continue
        fi

        printf '%s' "$value"
        return
    done
}

generate_env() {
    local env_file="$INSTALL_DIR/.env"
    if [ -f "$env_file" ]; then
        warn ".env already exists, keeping existing file"
        return
    fi

    info "Interactive setup: please answer the following questions."
    echo ""

    local bot_token bot_username admin_ids xui_url xui_username xui_password xui_cert_path db_pass

    bot_token=$(prompt_secret "Telegram Bot Token (from @BotFather)" "" "required")
    echo ""
    bot_username=$(prompt "Telegram Bot Username (without @)" "" "required")
    admin_ids=$(prompt "Admin Telegram IDs (comma-separated)" "" "required")
    xui_url=$(prompt "XUI Panel URL (e.g. https://panel.example.com:54321)" "" "required")
    xui_username=$(prompt "XUI Panel Username" "" "required")
    xui_password=$(prompt_secret "XUI Panel Password" "" "required")
    echo ""
    xui_cert_path=$(prompt "XUI Certificate Path (leave empty if not needed)" "")

    db_pass=$(openssl rand -base64 32 2>/dev/null || tr -dc A-Za-z0-9 </dev/urandom | head -c 32)

    cat > "$env_file" <<EOF
# Telegram Bot
BOT_TOKEN=${bot_token}
BOT_USERNAME=${bot_username}
ADMIN_IDS=${admin_ids}

# XUI Panel
XUI_URL=${xui_url}
XUI_USERNAME=${xui_username}
XUI_PASSWORD=${xui_password}
XUI_CERT_PATH=${xui_cert_path}

# Database: sqlite | postgres
DB_TYPE=postgres
DB_HOST=db
DB_PORT=5432
DB_NAME=xuibot
DB_USER=xuibot
DB_PASSWORD=${db_pass}
EOF

    ok ".env created at $env_file"
}

copy_project() {
    info "Copying project to $INSTALL_DIR..."
    mkdir -p "$INSTALL_DIR"

    # If running from git repo, copy all files. Otherwise clone.
    if [ -d "$SCRIPT_DIR/.git" ] || [ -f "$SCRIPT_DIR/pom.xml" ]; then
        cp -a "$SCRIPT_DIR"/. "$INSTALL_DIR"/
    else
        git clone https://github.com/user/xui-java.git "$INSTALL_DIR" 2>/dev/null || true
    fi

    # Ensure necessary directories exist
    mkdir -p "$INSTALL_DIR/data/postgres"
    mkdir -p "$INSTALL_DIR/data/npm-data"
    mkdir -p "$INSTALL_DIR/data/npm-letsencrypt"
    mkdir -p "$INSTALL_DIR/data/portainer"
    mkdir -p "$INSTALL_DIR/logs"
    mkdir -p "$INSTALL_DIR/site"

    ok "Project copied to $INSTALL_DIR"
}

do_install() {
    check_root
    install_docker
    copy_project
    generate_env

    ok "Installation complete!"
    echo ""
    echo -e "${GREEN}Next steps:${NC}"
    echo "  Run ${YELLOW}$0 start${NC} to launch everything"
    echo ""
    echo -e "${GREEN}Nginx Proxy Manager${NC} will be available at:"
    echo "  http://YOUR_SERVER_IP:81"
    echo "  Default login: admin@example.com / changeme"
    echo ""
    echo -e "${GREEN}Portainer${NC} will be available at:"
    echo "  http://YOUR_SERVER_IP:9000"
}

do_start() {
    if [ ! -d "$INSTALL_DIR" ]; then
        err "Not installed. Run: $0 install"
        exit 1
    fi
    cd "$INSTALL_DIR"
    info "Building and starting containers..."
    docker compose up -d --build
    ok "Started!"
    info "Bot logs: $0 logs"
    info "NPM admin panel: http://$(hostname -I | awk '{print $1}'):81"
}

do_debug() {
    if [ ! -d "$INSTALL_DIR" ]; then
        err "Not installed. Run: $0 install"
        exit 1
    fi
    cd "$INSTALL_DIR"
    info "Starting in DEBUG mode (foreground logs)..."
    export LOG_LEVEL=DEBUG
    docker compose up --build
}

do_stop() {
    cd "$INSTALL_DIR"
    info "Stopping containers..."
    docker compose down
    ok "Stopped"
}

do_restart() {
    cd "$INSTALL_DIR"
    info "Restarting containers..."
    docker compose restart
    ok "Restarted"
}

do_status() {
    cd "$INSTALL_DIR"
    docker compose ps
}

do_logs() {
    cd "$INSTALL_DIR"
    docker compose logs -f --tail=100 "${1:-bot}"
}

do_update() {
    check_root
    cd "$INSTALL_DIR"
    info "Updating..."
    docker compose down
    if [ -d ".git" ]; then
        git pull || warn "Git pull failed, continuing with local files"
    fi
    docker compose up -d --build
    ok "Updated"
}

do_uninstall() {
    check_root
    if [ ! -d "$INSTALL_DIR" ]; then
        err "Not installed."
        exit 1
    fi
    read -r -p "Are you sure you want to uninstall? ALL data in $INSTALL_DIR will be deleted! [y/N] " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
        cd "$INSTALL_DIR"
        docker compose down -v 2>/dev/null || true
        cd /
        rm -rf "$INSTALL_DIR"
        ok "Uninstalled"
    else
        info "Cancelled"
    fi
}

# --- Site only ---
do_site_start() {
    if [ ! -d "$INSTALL_DIR" ]; then
        err "Not installed. Run: $0 install"
        exit 1
    fi
    cd "$INSTALL_DIR"
    info "Starting site..."
    docker compose up -d site
    ok "Site started"
}

do_site_stop() {
    cd "$INSTALL_DIR"
    info "Stopping site..."
    docker compose stop site
    ok "Site stopped"
}

do_site_restart() {
    cd "$INSTALL_DIR"
    info "Restarting site..."
    docker compose restart site
    ok "Site restarted"
}

do_site_status() {
    cd "$INSTALL_DIR"
    docker compose ps site
}

do_site_logs() {
    cd "$INSTALL_DIR"
    docker compose logs -f --tail=100 site
}

do_site_update() {
    check_root
    cd "$INSTALL_DIR"
    info "Updating site..."
    docker compose stop site
    docker compose rm -f site 2>/dev/null || true
    docker compose build --no-cache site
    docker compose up -d site
    ok "Site updated"
}

# --- Bot only ---
do_bot_start() {
    if [ ! -d "$INSTALL_DIR" ]; then
        err "Not installed. Run: $0 install"
        exit 1
    fi
    cd "$INSTALL_DIR"
    info "Starting bot..."
    docker compose up -d bot
    ok "Bot started"
}

do_bot_stop() {
    cd "$INSTALL_DIR"
    info "Stopping bot..."
    docker compose stop bot
    ok "Bot stopped"
}

do_bot_restart() {
    cd "$INSTALL_DIR"
    info "Restarting bot..."
    docker compose restart bot
    ok "Bot restarted"
}

do_bot_status() {
    cd "$INSTALL_DIR"
    docker compose ps bot
}

do_bot_logs() {
    cd "$INSTALL_DIR"
    docker compose logs -f --tail=100 bot
}

do_bot_update() {
    check_root
    cd "$INSTALL_DIR"
    info "Updating bot..."
    docker compose stop bot
    docker compose rm -f bot 2>/dev/null || true
    docker compose build --no-cache bot
    docker compose up -d bot
    ok "Bot updated"
}

case "${1:-}" in
    install)
        do_install
        ;;
    start)
        do_start
        ;;
    debug)
        do_debug
        ;;
    stop)
        do_stop
        ;;
    restart)
        do_restart
        ;;
    status)
        do_status
        ;;
    logs)
        shift
        do_logs "$@"
        ;;
    update)
        do_update
        ;;
    uninstall)
        do_uninstall
        ;;
    site-start)
        do_site_start
        ;;
    site-stop)
        do_site_stop
        ;;
    site-restart)
        do_site_restart
        ;;
    site-status)
        do_site_status
        ;;
    site-logs)
        do_site_logs
        ;;
    site-update)
        do_site_update
        ;;
    bot-start)
        do_bot_start
        ;;
    bot-stop)
        do_bot_stop
        ;;
    bot-restart)
        do_bot_restart
        ;;
    bot-status)
        do_bot_status
        ;;
    bot-logs)
        do_bot_logs
        ;;
    bot-update)
        do_bot_update
        ;;
    *)
        echo "Usage: $0 <command>"
        echo ""
        echo "Global commands:"
        echo "  install    - Full installation (Docker, project files, interactive .env setup)"
        echo "  start      - Build and start all containers"
        echo "  stop       - Stop all containers"
        echo "  restart    - Restart all containers"
        echo "  status     - Show container status"
        echo "  logs       - Show logs (default: bot, use 'npm' or 'db' for others)"
        echo "  update     - Pull latest code and rebuild all"
        echo "  uninstall  - Remove everything including data"
        echo ""
        echo "Site only:"
        echo "  site-start    - Start site container"
        echo "  site-stop     - Stop site container"
        echo "  site-restart  - Restart site container"
        echo "  site-status   - Show site container status"
        echo "  site-logs     - Show site logs"
        echo "  site-update   - Rebuild and update site container"
        echo ""
        echo "Bot only:"
        echo "  bot-start     - Start bot container"
        echo "  bot-stop      - Stop bot container"
        echo "  bot-restart   - Restart bot container"
        echo "  bot-status    - Show bot container status"
        echo "  bot-logs      - Show bot logs"
        echo "  bot-update    - Rebuild and update bot container"
        exit 1
        ;;
esac
