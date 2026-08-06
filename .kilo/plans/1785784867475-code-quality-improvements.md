# План улучшений кода и архитектуры afk-helper

## Контекст
Проект уже хорошо структурирован: слоение controller→service→repository→domain + client, Konsist-тесты
архитектуры, единый `ApiLogger`, `@RestControllerAdvice`, переиспользование сервисов между REST и Telegram,
unit + IT покрытие.

Цель плана — убрать нарушения SOLID/DRY/KISS без изменения **модели конкурентности** (по решению владельца
оставляем текущую связку корутины `runBlocking` в `CodeActivationService` + Spring `codeActivationExecutor`).

## Затронутые границы
- `service/` — рефакторинг и выделение сервисов, событий, единой иерархии ошибок.
- `controller/` — упрощение `ApiExceptionHandler`, перенос маппинга entity→DTO из контроллера.
- `telegram/` — переиспользование единого маппера ошибок.
- `config/` / `client/` — вынос секретов из `application.yml`.
- `src/test/` — обновление/дополнение unit-тестов и Konsist-правил.

**Вне объёма** (осознанно): смена модели конкурентности, metrik/actuator, пагинация/новые эндпоинты.

---

## Упорядоченный список задач

### 1. Безопасность: секреты вне репозитория
- В `src/main/resources/application.yml:26` закоммичен реальный Telegram-токен `telegram.bot.token`.
  - Заменить жёстко прописанное значение на ссылку на env: `token: ${TELEGRAM_BOT_TOKEN:}`.
  - Аналогично подстраховать datasource-пароль (`${DB_PASSWORD:afk}`) — опционально.
- Добавить/обновить `.env.example` или секцию в AGENTS.md с перечнем требуемых переменных.
- Проверить, нет ли других секретов в `git ls-files` (в т.ч. забранных из истории — вне скоупа, только фикс на будущее).

### 2. Единая иерархия ошибок (DRY + SOLID)
- Ввести базовый класс `service/ApiException.kt`:
  ```kotlin
  sealed/open class ApiException(val errorCode: String, val httpStatus: HttpStatus, message: String?) : RuntimeException(message)
  ```
- Перевести существующие исключения на наследников:
  - `InvalidCodeException` → `ApiException("INVALID_CODE", BAD_REQUEST, ...)`
  - `CodeAlreadyExistsException` → `ApiException("CODE_ALREADY_EXISTS", CONFLICT, ...)`
  - `LilithApiException` → `ApiException(errorCode ?: "LILITH_API_ERROR", BAD_GATEWAY, message)` (оставить поле `errorCode`).
- Упростить `ApiExceptionHandler`:
  - Один общий `@ExceptionHandler(ApiException::class)` → строит `ResponseEntity` из `errorCode`/`httpStatus`/`message`.
  - Оставить отдельные хэндлеры только для фреймворковых (`MethodArgumentNotValidException`,
    `HttpMessageNotReadableException`, `HttpMediaTypeNotSupportedException`, `RestClientException`).
- Убрать дублирование маппинга ошибок в `TelegramMessageHandler`: вместо набора `catch` на каждое
  исключение собрать зарегистрированные случаи через единый сервис-маппер
  `ErrorResponseMapper` (возвращает `ApiError` либо человекочитаемый текст). И REST (через `ApiExceptionHandler`),
  и Telegram используют одну функцию `ApiError → message`.

### 3. Избавиться от поддельного HTTP-DTO в сервисном слое (DIP)
- Проблема: `RedemptionAutoApplyService` переиспользует `CodeActivationService.activate(ActivateCodeRequest)`
  через фиктивный `authCode = null` — сервисный слой зависит от HTTP-DTO.
- Добавить в `CodeActivationService` (или выделить фасад) перегрузку доменного уровня:
  `fun activate(moneyUid: String, redemptionCode: String): CodeActivationResponse`
  без `authCode` (при незарегистрированном аккаунте бросает `InvalidCodeException`).
  Внутри вызывает общий приватный конвейер.
- `RedemptionAutoApplyService` переводится на доменную сигнатуру. HTTP-DTO остаётся только на входе контроллера.
- (Опционально) переименовать параметр `uid` → `accountUid`, чтобы не путать с `character.uid: Long`.

### 4. Разбивка «божественного» `CodeActivationService` (SRP), без смены конкурентности
- Выделить `service/AccountService.kt`: `ensureAccount(uid, authCode)`, регистрация с обработкой
  `DataIntegrityViolationException`, проверка `requireAuthCode`.
- Выделить `service/CharacterSyncService.kt`: `syncCharacters(accountId, roles)` — текущий upsert
  (`saveAll` + `associateBy`).
- `CodeActivationService` оставляет оркестрацию: `requireActiveCode` → аккаунт → роли → синк → параллельная
  активация персонажей (текущий `runBlocking` сохраняем по решению владельца) и обработку
  `LilithApiException`/`RestClientException` → `CodeActivationResponse.Result`.

### 5. Событийная модель автоприменения вместо ручного TransactionSynchronization (SRP/DIP)
- Текущее: `RedemptionCodeService.saveRedemptionCode` + `scheduleAutoApply` с ручным
  `TransactionSynchronizationManager.registerSynchronization`.
- Заменить на идиоматичный механизм:
  - Создать `RedemptionCodeAddedEvent` (data-класс с `code: RedemptionCode`).
  - `RedemptionCodeService` после коммита публикует событие через `ApplicationEventPublisher`
    (без ручного `afterCommit`; событие может быть `@TransactionalEventListener`).
  - Новый `@Component RedemptionAutoApplyEventListener` с
    `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `@Async("codeActivationExecutor")`,
    вызывающий `RedemptionAutoApplyService`.
- `RedemptionCodeService` больше **не зависит** от `RedemptionAutoApplyService` (DIP).
- Сохранить поведение при отсутствии активной транзакции (unit-тесты) — обрабатывать через
  `if (TransactionSynchronizationManager.isSynchronizationActive())` при публикации либо документировать/покрыть тестом.

### 6. Надёжное детектирование duplicate key (DRY/надёжность)
- Убрать хрупкое `e.mostSpecificCause.message?.contains("unique")`.
- Ввести `service/DuplicateKeyDetector.kt` (или утилиту): определяет конфликт по `PSQLException`/`SQLException`
  state `23505` (и/или универсально через `DataIntegrityViolationException`), с fallback.
- Использовать в `RedemptionCodeService.saveRedemptionCode` и в `AccountService.ensureAccount`.

### 7. Устранение дублирования нормализации/валидации (DRY)
- Единый `service/RedemptionCodeNormalizer` уже есть — распространить единый подход на uid:
  - Вынести проверку/нормализацию `uid` (trim + NotBlank) в один помощник, используемый и сервисом,
    не дублируя в DTO-аннотациях и `normalizeUid`.
  - `normalizeUid` в `CodeActivationService` и `requireAuthCode` переехать в `AccountService`/валидатор.
- Убедиться, что семантика не задвоена: bean-валидация в DTO — транспортная проверка, бизнес-нормализация — в сервисе.
- (Опционально) вынести `CodeValidator` из `RedemptionCodeNormalizer`-object в инжектируемый компонент ради тестируемости.

### 8. Перенос маппинга entity→DTO из контроллера (KISS/SRP)
- Убрать приватный `toResponse()` из `RedemptionCodeController` и `requireNotNull(id)`.
- Маппинг `RedemptionCode → RedemptionCodeResponse` выполнять в `RedemptionCodeService` (после `save`, id гарантированно задан).
- `RedemptionCodeController` возвращает готовый DTO; контроллер остаётся тонким.

### 9. Тесты и архитектурные правила (валидация)
- Обновить Konsist: добавить правило для контроллеров — не зависеть от `client`; проверить слои после выделения
  `AccountService`, `CharacterSyncService`, event-пакета. При необходимости расширить `KonsistArchitectureTest`.
- Добавить/обновить unit-тесты:
  - `AccountService` (регистрация, гонка, duplicate key).
  - `CharacterSyncService` (upsert).
  - Событийный слушатель автоприменения (публикация события, `AFTER_COMMIT`, обработка при отсутствии транзакции).
  - `ErrorResponseMapper` и единая иерархия `ApiException` (в т.ч. статусы в `ApiExceptionHandler`).
- Обновить существующие тесты, если сигнатуры сваны: `CodeActivationServiceTest`, `RedemptionAutoApplyServiceTest`,
  `RedemptionCodeServiceTest`, `TelegramMessageHandlerTest`.

---

## Порядок выполнения (безопасный инкремент)
1. Задача 2 (иерархия ошибок + хэндлер) — независима, низкий риск, сразу упрощает код.
2. Задача 3 и 4 (доменная сигнатура + разбивка сервиса) — можно вместе, следить за тестами.
3. Задача 1 (секреты) и 6 (duplicate key) — точечные правки.
4. Задача 5 (события) — после 3/4; меняет связность автоприменения.
5. Задача 7 и 8 — финальные упрощения.
6. Задача 9 — тесты и Konsist после каждого этапа.

## Риски / замечания
- Шаг 5 меняет хронологию запуска автоприменения — внимательно покрыть тестом гарантию «после коммита» и поведение вне транзакции.
- Шаг 3/4 меняет публичные сигнатуры сервисов — затронуты REST-контроллеры (сигнатура `activate(ActivateCodeRequest)` остаётся), `TelegramMessageHandler`, `RedemptionAutoApplyService` и их тесты.
- Шаг 2 меняет классы исключений — проверить, что `@ExceptionHandler`-маппинг и `TelegramMessageHandler` переведены согласованно.

## Критерии готовности
- `./gradlew build` (вкл. ktlint + detekt) и `./gradlew test` проходят «зелёным».
- Konsist-правила архитектуры проходят.
- В `application.yml` нет секретов; токен читается из env.
- Дублирование маппинга ошибок, ручной `TransactionSynchronization` и поддельный HTTP-DTO в сервисе устранены.
