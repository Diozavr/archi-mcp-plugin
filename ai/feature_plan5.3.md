### План внедрения: MCP JSON‑RPC — обязательные методы (заглушки) (feature-plan 5.3)

Цель:
- Дополнить MCP HTTP endpoint `/mcp` минимальным набором методов JSON‑RPC, чтобы клиенты (Cursor и пр.) стабильно подключались: `initialize`, `notifications/initialized`, `tools/list`, `tools/call`.
- На первом шаге реализовать заглушки (без глубокой логики), затем подменять заглушки реальными реализациями, маппящимися на текущие сервисы/инструменты.

—

Инварианты:
- Один endpoint: `POST /mcp` (JSON‑RPC 2.0), `Content-Type: application/json; charset=utf-8`.
- Любые уведомления (notification, без `id`) → HTTP 204; любые запросы с `id` → HTTP 200 + `{result|error}`.
- Batch (массив запросов) → массив ответов; если все уведомления — HTTP 204.

—

Шаг 1. Контроллер JSON‑RPC: каркас методов
- Ввести/обновить `McpHttpHandler` с маршрутизацией `method`:
  - `initialize` → вернуть `{ protocolVersion, serverInfo, capabilities }`.
  - `notifications/initialized` → 204 (без тела).
  - `tools/list` → вернуть список инструментов с JSON Schema входных параметров.
  - `tools/call` → вызвать инструмент по имени, вернуть результат (пока — заглушки для всех инструментов, минимум `status`).
- Общая обработка ошибок:
  - неизвестный метод → `error.code = -32601, message = "Method not found"`.
  - неверные параметры → `error.code = -32602`.
  - иные нештатные → `error.code = -32000`.
- Статус: [done]
- Сделано: реализован `JsonRpcHttpHandler` с маршрутизацией методов, общей обработкой ошибок и поддержкой одиночных запросов.
- Осталось: —

Шаг 2. Заглушки ответов
- `initialize` (response):
  ```json
  {
    "protocolVersion": "2024-11-05",
    "serverInfo": { "name": "Archi MCP", "version": "0.1.0" },
    "capabilities": {
      "tools": { "listChanged": false },
      "prompts": false,
      "resources": false,
      "logging": { "levels": ["info","warn","error"] }
    }
  }
  ```
- `notifications/initialized`: 204.
- `tools/list`: вернуть инструменты из реестра (если нет —  пустой список). Для каждого инструмента отдать:
  - `name`, `description`, `inputSchema` (`type: object`, `properties`, `required`, `additionalProperties: false`).
  - Использовать уже существующие описатели параметров (например, `ToolParam`) и собрать JSON Schema (пока минимальный типовой маппинг: `string|number|integer|boolean`).
- `tools/call`:
  - Если инструмент не найден → `error -32601`.
  - Временно поддержать минимум — `status` (возвращает `{ok:true, service:\"Archi MCP\"}`) и `get_view_image` (делегировать на существующий REST‑хендлер и упаковать ответ как `{content_type, data_base64, bytes}`).
- Статус: [done]
- Сделано: реализованы заглушки `initialize`, `notifications/initialized`, `tools/list`, `tools/call` (`status`, `get_view_image`).
- Осталось: —

Шаг 3. Реестр инструментов
- Создать/дополнить `ToolRegistry`:
  - Регистрация инструментов: имя → обработчик (лямбда/класс) + метаданные (описание, параметры). 
  - Для начала зарегистрировать: `status`, `list_views`, `create_view`, `get_view_content`, `create_element`, `add_element_to_view`, `save_model`, `get_view_image`, `search`, `types`, `folders`, `folder_ensure`.
  - Параметры инструментов описывать через `ToolParam` и конвертировать в JSON Schema для `tools/list`.
- Статус: [done]
- Сделано: введён `ToolRegistry` с перечисленными инструментами и генерацией JSON Schema параметров.
- Осталось: —

Шаг 4. Batch‑поддержка и нотификации
- Если тело — массив запросов: обработать каждый и вернуть массив ответов (сохранив порядок). Уведомления без `id` пропускать; если ответы пусты → HTTP 204.
- Если запрос — нотификация (нет `id`) для `notifications/initialized` → 204; для прочих — также 204 (или 200 с `error` по желанию клиента, но лучше 204).
- Статус: [done]
- Сделано: `JsonRpcHttpHandler` обрабатывает батч‑запросы и уведомления, возвращая 204 при отсутствии ответов.
- Осталось: —

Шаг 5. Юнит‑тесты (минимум)
- Прогнать через тестовый контроллер JSON‑RPC:
  - `initialize` → 200 + `result.protocolVersion`.
  - `notifications/initialized` → 204.
  - `tools/list` → 200 + массив `tools` (не пуст при наличии реестра).
  - `tools/call` неизвестного инструмента → 200 + `error.code=-32601`.
  - Batch (смешанный: 1 запрос + 1 нотификация) → массив ответов.
- Статус: [partial]
- Сделано: добавлены unit‑тесты `JsonRpcHttpHandlerTest` для основных сценариев (`initialize`, нотификации, batch, `tools/list`, ошибки параметров).
- Осталось: запустить тесты в среде с зависимостями Archi.

Шаг 6. Smoke‑проверки
- Обновить `test/test_smoke.sh` (уже содержит базовые RPC‑вызовы) при необходимости под новые ответы.
- Проверить совместимость с Cursor MCP: ошибка про `initialize` должна исчезнуть.
- Статус: [partial]
- Сделано: `test/test_smoke.sh` дополнен вызовами JSON‑RPC.
- Осталось: выполнить скрипт с запущенным сервером и активной моделью.

Шаг 7. Документация
- `README.md`: краткий раздел «MCP JSON‑RPC методы (минимум)» с примерами.
- `AGENTS.md`: упоминание, что MCP внутри плагина реализует `initialize`/`tools.list`/`tools.call`/`notifications.initialized`.
- Статус: [done]
- Сделано: обновлены `README.md` и `AGENTS.md` с описанием MCP JSON‑RPC методов и примерами.
- Осталось: —

Шаг 8. Роллбэк
- Удалить `McpHttpHandler`/реестр/схемы; endpoint `/mcp` вернуть в 501 (Not Implemented).
- Статус: [todo]
- Сделано: —
- Осталось: описать и реализовать процедуру отката при необходимости.

Критерии готовности (DoD)
- `/mcp` корректно обрабатывает `initialize`, `notifications/initialized`, `tools/list`, `tools/call` (минимальные заглушки), а также batch‑запросы.
- Cursor/Codex больше не запрашивает несуществующий `initialize`.
- `tools/list` отдаёт валидный JSON Schema для параметров инструментов.


