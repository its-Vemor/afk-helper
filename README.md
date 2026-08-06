AFK helper

## Telegram-бот

Помимо REST API, командами можно управлять через Telegram-бота (long polling).
Настройка — в `application.yml`. Секреты задаются через переменные окружения:

| Переменная | Назначение |
|---|---|
| `TELEGRAM_BOT_TOKEN` | Токен бота от @BotFather (обязателен, если включён бот) |
| `DB_USERNAME` | Пользователь PostgreSQL (по умолчанию `afk`) |
| `DB_PASSWORD` | Пароль PostgreSQL (по умолчанию `afk`) |

```yaml
telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN:}
    enabled: true
```

Команды бота:

| Команда | Описание |
|---|---|
| `/add <код>` | Добавить новый код возмещения (переиспользует `RedemptionCodeService.addCode`) |
| `/activate <uid> <код> [authCode]` | Активировать код на персонажах аккаунта (переиспользует `CodeActivationService.activate`; `authCode` нужен только для незнакомого `uid`) |
| `/help` | Справка |

REST API и бот используют один и тот же сервисный слой.
Для отключения бота в окружениях без доступа к Telegram используйте
`telegram.bot.enabled=false` (и `telegrambots.enabled=false` для стартера).

## Логирование

В `application.yml` есть флаг `app.logging.enabled` — подробное логирование
вызовов и ответов для REST, Telegram и запросов к внешнему API Lilith:

```yaml
app:
  logging:
    enabled: true
```

- Вызовы/ответы логируются только при `enabled: true`
  (например: `[rest] REQUEST POST /api/codes params={code=AFK-001}`,
  `[lilith] RESPONSE /api/consume ...`).
- Ошибки логируются всегда (WARN с кодом ошибки и, где нужно, стектрейсом).
- Чувствительные значения (token, authCode, authorization, secret, password)
  маскируются как `[MASKED]`.

