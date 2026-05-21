package bot.util;

public final class Messages {

    private Messages() {}

    public static final String START_HELLO = """
            👋 Привет, <b>%s</b>!

            Я бот для управления VPN-ключами. Вот что я умею:

            🔑 <b>Купить ключ</b> — приобрести новый VPN-ключ
            💰 <b>Баланс</b> — посмотреть свой баланс
            💳 <b>Пополнить</b> — пополнить баланс через Telegram Stars
            📊 <b>Профиль</b> — вся информация о тебе

            Используй кнопки меню ниже 👇""";
    public static final String UNKNOWN_COMMAND = "Не понял команду. Используй кнопки меню.";
    public static final String NO_KEYS = "У тебя пока нет ключей.";
    public static final String KEYS_TITLE = "🔑 <b>Твои ключи</b> (%d):";
    public static final String KEY_NOT_FOUND = "Ключ не найден";
    public static final String TRAFFIC_RESET_OK = "Трафик сброшен";
    public static final String TRAFFIC_RESET_ERR = "Ошибка при сбросе трафика";
    public static final String KEY_DELETED = "Ключ удалён.";
    public static final String KEY_DELETE_ERR = "Ошибка при удалении с панели";
    public static final String REFRESHED = "Обновлено";
    public static final String TOPUP_TITLE = "Выбери сумму пополнения (в Telegram Stars ⭐):";
    public static final String INVOICE_TITLE = "Пополнение баланса";
    public static final String INVOICE_DESC = "Пополнение на %d ⭐";
    public static final String PAYMENT_SUCCESS = "✅ Оплата прошла! Баланс пополнен на <b>%d ⭐</b>";
    public static final String ADMIN_PANEL = """
            ⚙️ <b>Админка</b>

            Доступные команды:
            /add_key — добавить ключ пользователю
            /xui_inbounds — список inbound'ов с панели
            /users — список пользователей""";
    public static final String XUI_NO_INBOUNDS = "Панель недоступна или нет inbound'ов.";
    public static final String XUI_INBOUND_NOT_FOUND = "Inbound не найден";
    public static final String ADD_KEY_ENTER_TG_ID = "Telegram ID пользователя:";
    public static final String ADD_KEY_NEED_NUMBER = "Нужен числовой ID.";
    public static final String ADD_KEY_USER_NOT_FOUND = "Пользователь не найден в БД.";
    public static final String ADD_KEY_NO_INBOUNDS = "Не удалось получить inbound'ы с панели.";
    public static final String ADD_KEY_ENTER_INBOUND = "\nВведи ID inbound'а:";
    public static final String ADD_KEY_ENTER_EMAIL = "Email для клиента (уникальный на панели):";
    public static final String ADD_KEY_ENTER_REMARK = "Remark (описание, можно пустым):";
    public static final String ADD_KEY_ENTER_EXPIRY = "Срок действия в днях (0 — бессрочно):";
    public static final String ADD_KEY_ENTER_TRAFFIC = "Лимит трафика в GB (0 — безлимит):";
    public static final String ADD_KEY_NEED_INT = "Число.";
    public static final String ADD_KEY_ERROR = "Ошибка при создании клиента на панели.";
    public static final String ADD_KEY_SUCCESS = """
            ✅ Ключ создан
            Email: <code>%s</code>
            UUID: <code>%s</code>""";
    public static final String NO_USERS = "Пользователей нет.";
    public static final String USERS_TITLE = "<b>Пользователи</b>:\n\n";
    public static final String CANCEL_OK = "Действие отменено.";
    public static final String BALANCE_TEXT = "💰 <b>Твой баланс:</b> %.2f ⭐";
    public static final String BUY_KEY_TITLE = "🛒 <b>Покупка ключа</b>\n\nВыбери inbound (сервер):";
    public static final String BUY_KEY_NO_INBOUNDS = "Нет доступных серверов для покупки. Попробуй позже.";
    public static final String BUY_KEY_SELECT_PLAN = "Выбери тариф для <b>%s</b>:";
    public static final String BUY_KEY_INSUFFICIENT_BALANCE = "❌ Недостаточно средств. Твой баланс: <b>%.2f ⭐</b>, а нужно <b>%d ⭐</b>.\n\nПополни баланс через меню 💳 Пополнить.";
    public static final String BUY_KEY_SUCCESS = """
            ✅ <b>Ключ успешно куплен!</b>

            📧 Email: <code>%s</code>
            🔑 UUID: <code>%s</code>
            📡 Inbound: %s
            📅 Срок: %s
            📊 Трафик: %s

            Баланс списан: <b>%d ⭐</b>
            Остаток: <b>%.2f ⭐</b>

            Ключ появится в разделе 🔑 Мои ключи.""";
    public static final String BUY_KEY_ERROR = "❌ Ошибка при создании ключа на сервере. Попробуй позже или обратись к администратору.";
    public static final String BUY_KEY_PLAN_1 = "📅 30 дней / 100 GB — %d ⭐";
    public static final String BUY_KEY_PLAN_2 = "📅 90 дней / 500 GB — %d ⭐";
    public static final String SERVICE_UNAVAILABLE = "⚠️ Сервис VPN временно недоступен. Попробуй позже.";
}

