### План внедрения: MCP внутри плагина (HTTP JSON‑RPC) поверх общих сервисов (feature-plan 5)

Цель:
- Реализовать MCP‑интерфейс внутри плагина Archi как HTTP JSON‑RPC endpoint `/mcp/`, дополняя существующий REST API.
- Выделить общий слой (сервисы/DTO/валидации), чтобы REST и MCP использовали одни и те же методы и структуры.

—

Инварианты (см. `AGENTS.md`):
- Слушаем только `127.0.0.1` (локально), порт задаётся через `ARCHI_MCP_PORT`/`archi.mcp.port` (дефолт: 8765).
- Операции с SWT/EMF — строго через `Display.getDefault().syncExec(...)`.
- Истина REST‑контракта — `resources/openapi.json`; тексты MCP‑параметров должны быть с ней согласованы.
- Тесты размещаем под `com.archimatetool.mcp/test` внутри плагина [[memory:5587551]].

Предпосылки/зависимости:
- Рекомендуется выполнить шаги `feature_plan4` (Jackson для чтения/записи JSON) — MCP опирается на Jackson ObjectMapper.

—

Шаг 1. Спецификация MCP поверх HTTP JSON‑RPC 2.0
- Транспорт: `POST /mcp/` с `Content-Type: application/json; charset=utf-8`.
- Поддержать одиночные и батч‑запросы JSON‑RPC 2.0 (`id`, `method`, `params`).
- Методы уровня протокола:
  - `tools/list` — возвращает список инструментов, их описания и параметры (по аналогии с текущими MCP‑инструментами в `server.py`).
  - `tools/call` — вызов инструмента по имени с `params` (объект).
  - (опционально) `status/ping` — быстрая проверка.

Шаг 2. Общий слой: сервисы, DTO, валидаторы
- Использовать существующие сервисы: `ActiveModelService`, `ElementService`, `RelationService`, `ViewService`.
- Использовать DTO‑конвертеры из `ModelApi` (`elementToDto`, `relationToDto`, `viewToDto`, `viewContentToDto`, `connectionToDto`).
- Вынести общие проверки/валидации параметров в утилиты (минимум):
  - `JsonBody`/Jackson helpers для безопасного извлечения значений типов.
  - Общая утилита нормализации ID/enum/числовых параметров.
- Для REST ничего не меняем по контракту; MCP будет вызывать те же сервисы и собирать те же DTO.

Шаг 3. Реестр инструментов (ToolRegistry)
- Новый пакет, например: `com.archimatetool.mcp.mcp` с классами:
  - `Tool` (имя, описание, карта параметров: имя → метаданные/обязательность/тип/описание).
  - `ToolRegistry` (регистрация/получение инструментов).
  - `ToolInvoker` (интерфейс/бифункция для вызова; имплементации обёртывают существующие сервисные методы).
- Инициализация реестра при старте бандла (в `Activator`/`Startup`).
- Описания параметров перенести из `archi-mcp-server/server.py` (`PARAM_DESCRIPTIONS`) и синхронизировать с `openapi.json`.

Шаг 4. Контроллер JSON‑RPC (McpHttpHandler)
- Добавить HTTP‑обработчик `McpHttpHandler` и зарегистрировать контекст `/mcp/` в `Router`.
- Реализация:
  - Разбор запроса Jackson‑ом (поддержка массива/объекта).
  - Роутинг по `method`: `tools/list`, `tools/call`.
  - Обработка ошибок: JSON‑RPC error‑объекты (по спецификации), HTTP 200 всегда для успешного RPC‑ответа (даже с error внутри).
  - Таймауты/исполнитель: использовать уже настроенный `CachedThreadPool` сервера; потенциально ограничить длительные вызовы.

Шаг 5. Набор инструментов MCP (первый релиз)
- Отразить паритет с существующими возможностями REST:
  - `status`
  - `list_views`, `create_view`, `get_view`, `delete_view`, `get_view_content`, `get_view_image` (PNG/SVG)
  - `create_element`, `get_element`, `update_element`, `delete_element`
  - `create_relation`, `get_relation`, `update_relation`, `delete_relation`
  - `add_element_to_view`, `add_relation_to_view`, `update_object_bounds`, `move_object_to_container`, `remove_object_from_view`
  - `types`, `folders`, `folder_ensure`, `search`
- Каждому инструменту — параметры, дефолты и описания (синхронизировать с OpenAPI, без изменения бизнес‑логики).

Шаг 6. Конфигурация и совместимость
- Путь по умолчанию: `http://127.0.0.1:8765/mcp/`.
- Для совместимости с текущей IDE‑настройкой можно задать порт `8081` через `ARCHI_MCP_PORT`/`archi.mcp.port` и оставить путь `/mcp/`.
- Никакой внешней аутентификации (локальная петля), как и для REST.

Шаг 7. Тесты
- Юнит‑тесты JSON‑RPC:
  - Разбор одиночного и батч‑запросов; корректные/битые payload; error‑ветки (unknown method, wrong params).
  - `tools/list` — содержит ожидаемые инструменты и параметры.
  - `tools/call` — happy‑path для нескольких инструментов (status, list_views, search).
- Smoke‑проверки (WSL):
  ```bash
  # tools/list
  curl -sS -X POST http://127.0.0.1:${ARCHI_MCP_PORT:-8765}/mcp/ \
    -H 'Content-Type: application/json' \
    -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | jq .

  # tools/call: get_view_image (SVG)
  VID="<view-id>"
  curl -sS -X POST http://127.0.0.1:${ARCHI_MCP_PORT:-8765}/mcp/ \
    -H 'Content-Type: application/json' \
    -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"get_view_image","args":{"view_id":"'"$VID"'","format":"svg"}}}' \
    | jq -r .result.data_base64 | base64 -d > /tmp/view.svg && head -n2 /tmp/view.svg
  ```

Шаг 8. Документация
- `README.md`: раздел «MCP внутри плагина», пример `.cursor/mcp.json` с `url: "http://127.0.0.1:8765/mcp/"`.
- `AGENTS.md`: добавить памятку по включению MCP и синхронизации описаний параметров с OpenAPI.
- Если порт меняется — зафиксировать способ конфигурации.

Шаг 9. Поддержка и расширения
- Логи MCP: перевести на `org.eclipse.core.runtime.ILog`.
- Ограничения выполнения/таймауты: конфиг через `Config` (пер‑запуск не обязателен).
- При необходимости — добавить ещё инструменты, оставаясь на общем сервисном слое без дублирования логики.

Шаг 10. Роллбэк
- Удалить контекст `/mcp/`, классы реестра/контроллера MCP, документацию.
- REST API остаётся без изменений.

Критерии готовности (DoD)
- MCP endpoint `/mcp/` доступен локально, `tools/list` и базовые `tools/call` работают.
- Инструменты MCP покрывают существующий функционал REST; результаты идентичны по структурам DTO.
- Тесты под `com.archimatetool.mcp/test` зелёные; smoke‑проверки проходят.


