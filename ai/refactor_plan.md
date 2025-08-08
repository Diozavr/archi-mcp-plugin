### План рефакторинга `archi-mcp-plugin`

Этот файл — единый источник правды для рефакторинга. Работа ведётся по шагам; после завершения каждого шага он помечается выполненным и добавляются заметки.

—

Инварианты (см. `AGENTS.md`):
- Слушаем только `127.0.0.1:8765`.
- Любые изменения EMF‑модели — через `Display.getDefault().syncExec(...)`.
- Сохраняем legacy‑маршруты (`/views/content?id=...`, `/views/add-element`).
- Не расширяем сетевую поверхность/безопасность без согласования.
- Уходим от regex‑парсинга JSON: используем `minimal-json` для входящих тел; формат ответов не меняем.

Процесс выполнения:
- Двигаемся строго по шагам ниже (1 → …).
- На каждый шаг: делаем минимальные совместимые изменения, прогоняем unit‑тесты и smoke‑проверки, затем отмечаем чекбокс и пишем короткие заметки (что сделано/риски).
- Коммиты атомарные, понятные: `refactor(step-01): ...` и т.п.

Smoke‑проверки (WSL, без интерактива):
```bash
curl -sS http://127.0.0.1:8765/status | jq .
curl -sS http://127.0.0.1:8765/types | jq .
curl -sS http://127.0.0.1:8765/folders | jq .
# Остальные см. шаги ниже
```

Критерии готовности всего рефакторинга:
- Классы‑хендлеры и сервисы ≤ 200–300 LOC, чёткие ответственности.
- Все маршруты (включая legacy) совместимы, ответы не менялись.
- Все модифицирующие операции идут через `UiExec.sync(...)`.
- Юнит‑тесты (без Archi) зелёные; smoke‑скрипты проходят.

—

Шаг 1. Инфраструктура JSON и утилиты
- Цель: Подключить парсер, не меняя формат ответов.
- Действия:
  - Добавить `com.eclipsesource.minimal-json` (например, `minimal-json-0.9.5.jar`) в `com.archimatetool.mcp/lib/`.
  - Обновить `META-INF/MANIFEST.MF` (`Bundle-ClassPath: ., lib/minimal-json-0.9.5.jar`).
  - Обновить `build.properties` (`bin.includes` добавить `lib/`).
  - Добавить `JsonReader` (обёртка: `optString/optInt/optBool/optObject/optIntWithin`).
  - Добавить `Config` (порт, DEBUG: sysprop → env → default) и `UiExec.sync(...)`.
- Unit‑тесты (plain JUnit): `JsonReaderTest`, `JsonUtilTest`, `ToCamelCaseTest` (перенести `toCamelCase` в утилиту без зависимостей).
 - Unit‑тесты (plain JUnit): `JsonReaderTest`, `JsonUtilTest`, `ToCamelCaseTest` (утилита `StringCaseUtil.toCamelCase`).
- Примечание: тесты размещены внутри `com.archimatetool.mcp` в каталоге `test` (plain JUnit, без Archi); проект настроен на сборку `src/` и `test/`.
- Smoke: `GET /status`.
- Критерий приёмки: сборка плагина успешна, тесты зелёные, `GET /status` работает.
- Статус: [x] Выполнено частично — добавлен `minimal-json-0.9.5.jar`, обновлены `MANIFEST.MF` и `build.properties`, добавлены `JsonReader`, `Config`, `UiExec`. Далее: написать базовые unit‑тесты и прогнать smoke `GET /status`.

Шаг 2. Router и разнос хендлеров
- Цель: Уменьшить размер `HttpServerRunner` и упростить тестирование.
- Действия:
  - Ввести `Router` (регистрация всех путей и legacy‑алиасов, 405 по умолчанию).
  - Разнести вложенные хендлеры в отдельные классы (без изменения логики/ответов).
  - Ввести `ResponseUtil` для единообразной отправки JSON/статусов.
- Unit‑тесты: `RouterPathParseTest`, `ResponseUtilTest`, `QueryParserTest`.
- Smoke: `GET /openapi.json`, `GET /types`, `GET /folders`.
- Критерий приёмки: функционально эквивалентно текущему поведению.
- Статус: [x] Выполнено частично — добавлены `Router`, `ResponseUtil`, вынесены `StatusHttpHandler`, `OpenApiHttpHandler`, `NotImplementedHttpHandler`, `TypesHttpHandler`, `FoldersHttpHandler`, `FolderEnsureHttpHandler`, `ElementsHttpHandler`, `ElementItemHttpHandler`, `RelationsHttpHandler`, `RelationItemHttpHandler`, `ViewsHttpHandler`, `ViewItemHttpHandler`, `ModelSaveHttpHandler`; legacy `/views/content` и `/views/add-element` временно оставлены из старого класса. Далее: перенос `/search` в отдельный хендлер и удаление старых вложенных классов.

Шаг 3. Замена парсинга тел на `JsonReader`
- Цель: Уйти от regex‑парсинга без изменения контрактов.
- Действия (точечно в хендлерах):
  - `POST /elements`, `POST /relations`, `POST /views`.
  - `/views/{id}/add-element` — чтение `elementId` и `bounds.{x,y,w,h}` (+ плоские `x,y,w,h`).
  - `PATCH /views/{id}/objects/{objectId}/bounds` — частичный патч `x|y|w|h`.
  - `POST /folder/ensure` — поле `path`.
- Unit‑тесты: расширить `JsonReaderTest`, `BoundsPatchValidatorTest`.
- Smoke: создать элемент/связь/вид, добавить элемент на вид (новый и legacy), патчить bounds, ensure folder.
- Критерий приёмки: ответы/коды совпадают, совместимость сохранена.
- Статус: [x] Выполнено частично — переведены `Elements`, `Relations`, `Views`, `ElementItem (PATCH only)`, `ViewItem (add-element + bounds)` и `FolderEnsure` на `JsonReader`. Далее: при необходимости доработать оставшиеся места и финальная проверка совместимости.

Шаг 4. Декомпозиция `ModelApi` в сервисы
- Цель: Читаемость и тестопригодность.
- Действия:
  - Выделить: `ActiveModelService`, `ElementService`, `RelationService`, `ViewService`, `FolderService`, `SearchService`, `TypesService`.
  - Все модификации — через `UiExec.sync(...)`.
  - Хендлеры переводим на сервисы.
- Unit‑тесты: поверх простых POJO/фейков — `SearchPredicateTest`, утилиты без SWT/EMF.
- Smoke: базовые CRUD и `/search`.
- Критерий приёмки: функционально эквивалентно, классы стали меньше.
- Статус: [x] В процессе — добавлены сервисы: `ActiveModelService`, `ElementService`, `RelationService`, `ViewService`, а также `ServiceRegistry`. Переведены все хендлеры на сервисы; legacy-хендлеры используют `JsonReader`. В `ElementService`/`RelationService` обеспечена очистка в видах через вызовы `remove*Occurrences`. Далее: оставить в `ModelApi` только DTO/вспомогательные методы.

Шаг 5. Единая обработка ошибок
- Цель: Консистентные ответы об ошибках.
- Действия: общий фильтр/обёртка для хендлеров, формируем `{"ok":false,"error":"..."}`; 409/400/404/405/501.
- Unit‑тесты: маппинг исключений → коды/тела, пустые тела на 204.
- Smoke: спровоцировать 400/404/405/409/501.
- Критерий приёмки: одинаковый формат ошибок, действующие коды.
- Статус: [ ] Не начато

Шаг 6. DTO‑слой для ответов
- Цель: Упростить формирование ответов и эволюцию.
- Действия: добавить DTO (`ElementDto`, `RelationDto`, `ViewDto`, `ViewObjectDto`, `ConnectionDto`, `FolderNodeDto` и др.), конвертеры → Map для совместимости с текущим `JsonUtil`.
- Unit‑тесты: преобразование DTO → Map → JSON (совпадение с текущим форматом).
- Smoke: выборочно все маршруты.
- Критерий приёмки: идентичный формат JSON.
- Статус: [ ] Не начато

Шаг 7. Preferences/Config
- Цель: Централизовать конфигурацию и подготовить UI.
- Действия: `Config` учитывает sysprop/env (и в будущем — Preferences). В `MCPPreferencePage` — информативный экран, без изменения рантайма.
- Unit‑тесты: `Config` — приоритеты источников, значения по умолчанию.
- Smoke: старт плагина, лог порта/DEBUG.
- Критерий приёмки: текущее поведение без изменений, код чище.
- Статус: Не надо делать!

Шаг 8. Логирование/диагностика
- Цель: Наглядные логи без шума.
- Действия: единый логгер/флаги, аккуратная статистика `/search` (сохранить поведение).
- Unit‑тесты: формат вспомогательных сообщений (по возможности), не завязываться на stdout.
- Smoke: посмотреть логи старта/поиска при DEBUG.
- Критерий приёмки: читаемые логи, без изменения API.
- Статус: Не надо делать!

Шаг 9. Документация и скрипты
- Цель: Обновить README и добавить примеры запросов.
- Действия: дополнить `README.md` (разделы тестов), добавить/обновить WSL‑скрипты для smoke.
- Unit‑тесты: —
- Smoke: прогон скриптов.
- Критерий приёмки: документация отражает текущее состояние.
- Статус: [ ] Не начато

Шаг 10. Финальная чистка
- Цель: Удалить мёртвый код, привести стиль, проверить лицензии.
- Действия: ревизия импортов, форматирование, удаление устаревших утилит.
- Unit‑тесты: полный прогон.
- Smoke: полный прогон.
- Критерий приёмки: зелёные тесты, чистый проект.
- Статус: [ ] Не начато

—

Журнал прогресса
- 2025‑08‑08: добавлен парсер minimal-json, настроены classpath/build для тестов в `com.archimatetool.mcp/test`, добавлены `JsonReader`, `Config`, `UiExec`, базовые unit‑тесты зелёные.
- 2025‑08‑08: вынесены хендлеры в `http.handlers`, добавлен `Router`, подключены legacy‑маршруты; добавлен `SearchHttpHandler`.
- 2025‑08‑08: удалён дублирующий тестовый проект `com.archimatetool.mcp.tests`; все тесты теперь в `com.archimatetool.mcp/test`.
- 2025‑08‑08: начата финальная чистка `HttpServerRunner` — переведено разрешение порта/DEBUG на `Config`; далее удалить вложенные хендлеры и regex‑парсинг.


