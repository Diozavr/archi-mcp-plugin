### План реализации: полный MCP сервер в Archi (feature-plan 5.2)

Цель:
- Завершить внедрение MCP внутри плагина, обеспечив полный паритет с REST API, описанным в `resources/openapi.json`.
- Предоставить интерфейс JSON‑RPC 2.0 на `POST /mcp/` с теми же сервисами и DTO, что и REST.

Инварианты (см. `AGENTS.md`):
- Сервер слушает только `127.0.0.1:${ARCHI_MCP_PORT:-8765}`.
- Любые изменения EMF выполняются через `Display.getDefault().syncExec(...)`.
- Описание параметров инструментов синхронизируется с `resources/openapi.json`.

---

## Шаги

1. **Протокол JSON‑RPC**
   - Доработать `JsonRpcHttpHandler`: поддержка одиночных и батч‑запросов, полный набор ошибок JSON‑RPC (`-32601`, `-32602`, `-32603`).
   - Добавить метод `status/ping` для быстрой проверки доступности.

2. **Реестр инструментов**
   - Ввести `Tool`, `ToolRegistry`, `ToolInvoker` (или закончить существующую реализацию).
   - Заполнить реестр всеми инструментами, отражающими операции REST:
     `status`, `openapi`, `types`, `folders`, `folder_ensure`, `search`,
     `list_views`, `create_view`, `get_view`, `delete_view`, `get_view_content`,
     `get_view_image`, `create_element`, `get_element`, `update_element`,
     `delete_element`, `create_relation`, `get_relation`, `update_relation`,
     `delete_relation`, `add_element_to_view`, `add_relation_to_view`,
     `update_object_bounds`, `move_object_to_container`, `remove_object_from_view`,
     `model_save`.

3. **Параметры и описания**
   - Для каждого инструмента описать параметры и их типы, перепроверив с `openapi.json`.
   - Поддержать варианты имен (`view_id` ↔ `viewId` и т. п.) при необходимости.

4. **Методы протокола**
   - `tools/list` возвращает метаданные всех зарегистрированных инструментов.
   - `tools/call` вызывает инструмент по имени; параметры передаются как объект `args`.
   - Обработать ошибки ядра через JSON‑RPC `error.data`.

5. **Тесты**
   - Юнит‑тесты `JsonRpcHttpHandler`: одиночный и батч‑режим, неизвестный метод, некорректные параметры.
   - Тесты `tools/list` и нескольких инструментов (`status`, `search`, `get_view_image`).
   - Выделить отдельный smoke‑скрипт `test_mcp_smoke.sh` с примерами `tools/list` и `tools/call`.

6. **Документация**
   - `README.md`: инструкция по включению MCP, пример `.cursor/mcp.json` с `url: "http://127.0.0.1:8765/mcp/"`.
   - `AGENTS.md`: напоминание о синхронизации описаний параметров и о локальном биндинге.

7. **Готовность/DoD**
   - `POST /mcp/` отвечает на `tools/list` и `tools/call` для всех инструментов.
   - Результаты MCP совпадают по структурам DTO с REST.
   - Тесты и отдельный MCP smoke‑скрипт зелёные.

---

Критерии роллбэка:
- Удалить `ToolRegistry` и `JsonRpcHttpHandler`.
- REST API остаётся без изменений.
