### План внедрения: MCP внутри плагина (полный паритет с REST/OpenAPI) — feature-plan 5.2

## Цель
- Реализовать MCP (HTTP JSON‑RPC 2.0) endpoint внутри плагина Archi с функциональным паритетом к REST API, описанному в `resources/openapi.json`.
- Переиспользовать ядровой слой из feature-plan 5.1 для всех операций (единая реализация вне зависимости от протокола).

## Предпосылки/зависимости
- Выполнен рефакторинг по feature-plan 5.1: выделен ядровой слой (`core.*`), адаптеры REST тонкие.
- Библиотеки Jackson подключены (см. feature-plan 4). Тестовая инфраструктура доступна.

## Инварианты
- Локальный сервер: `127.0.0.1`, порт — из `Config.resolvePort()`.
- Операции, меняющие EMF‑модель, — через `Display.getDefault().syncExec(...)` (инкапсулировано в ядре).
- Паритет с REST по структурам DTO; различается только транспорт/формат ошибки.
- Тесты — под `com.archimatetool.mcp/test` [[memory:5587551]].

## Архитектура MCP
- JsonRpc слой:
  - `JsonRpcHttpHandler` (контекст `/mcp/`), поддержка одиночного и батч‑запроса.
  - Обработка ошибок по JSON‑RPC 2.0 (invalid request/params, internal error) + доменные ошибки ядра (map в `-32000..-32099`).
- Реестр инструментов:
  - `ToolRegistry`, `Tool`, `ToolInvoker`: декларативные описания методов (имя, параметры, типы, обязательность, описания), делегирование в ядро.
  - `tools/list`: инвентаризация инструментов и параметров.
- Ядро:
  - Переиспользование `core.*` команд/запросов; валидации/исключения — там же.
- Бинарные ответы:
  - Для изображений (PNG/SVG) — `data_base64` + `content_type` (например, `image/png`), длина в байтах.

## Паритет методов MCP (маппинг на OpenAPI)
- status → `status`
- types → `types`
- folders → `folders/list`
- folder_ensure(path) → `folders/ensure`
- views:
  - list_views → `views/list`
  - create_view(type,name) → `views/create`
  - get_view(id) → `views/get`
  - delete_view(id) → `views/delete`
  - get_view_content(id,limit?,offset?) → `views/get_content`
  - get_view_image(id,format=png,scale=1.0,dpi?,bg?,margin=0) → `views/get_image`
  - add_element_to_view(id,element_id,parent_object_id?,bounds{x,y,w,h}) → `views/add_element`
  - add_relation_to_view(id,relation_id,source_object_id?,target_object_id?,policy=auto,suppress_when_nested=true) → `views/add_relation`
  - update_object_bounds(id,object_id,x?,y?,w?,h?) → `views/update_bounds`
  - move_object_to_container(id,object_id,parent_object_id,bounds?) → `views/move_object`
  - remove_object_from_view(id,object_id) → `views/remove_object`
- elements:
  - create_element(type,name,folder_id?,properties?,documentation?) → `elements/create`
  - get_element(id,include="relations"?,include_elements?) → `elements/get`
  - update_element(id,name?,type?,folder_id?,properties?,documentation?) → `elements/update`
  - delete_element(id) → `elements/delete`
  - list_element_relations(id,direction=both,include_elements?) → `elements/list_relations`
- relations:
  - create_relation(type,source_id,target_id,name?,folder_id?,properties?,documentation?) → `relations/create`
  - get_relation(id) → `relations/get`
  - update_relation(id,name?,type?,properties?,documentation?) → `relations/update`
  - delete_relation(id) → `relations/delete`
- search:
  - search(q?,kind?,element_type?,relation_type?,model_id?,property[]=key=value,include_docs?,include_props?,limit?,offset?,debug?,log?) → `search`
- model:
  - save_model(model_id?,create_backup=true) → `model/save`

Примечания по именованию параметров:
- Для MCP используем `snake_case` (например, `element_id`, `parent_object_id`), отображаем 1:1 на core‑параметры.

## Формат JSON‑RPC
- Запрос: `{ "jsonrpc":"2.0", "id":<string|number|null>, "method":"<name>", "params":{...} }`
- Успех: `{ "jsonrpc":"2.0", "id":<id>, "result":{...} }`
- Ошибка: `{ "jsonrpc":"2.0", "id":<id|null>, "error":{ "code":<int>, "message":"...", "data":{...} } }`
- Батч: массив запросов; ответы — массивом (порядок может отличаться, но `id` сохраняется).

## Маппинг ошибок
- Core → JSON‑RPC:
  - BadRequest → `-32001`
  - NotFound → `-32004`
  - Conflict (no active model, ambiguous auto, busy) → `-32009`
  - Unprocessable → `-32022`
  - Иное (непредвиденное) → `-32603` (Internal error)
- Invalid JSON / invalid request → `-32700` / `-32600`
- Invalid params (семантика уровня RPC) → `-32602`

## Конкурентность и таймауты
- Использовать существующий пул исполнения HTTP‑сервера; операции ядра — короткие.
- При необходимости добавить мягкие таймауты для длинных операций (логирование прерываний).

## Шаги реализации
1) Контур JSON‑RPC
   - Добавить `JsonRpcHttpHandler`, зарегистрировать контекст `/mcp/` в `Router`.
   - Реализовать разбор одиночного/батч‑запросов, общую схему ответа.
   - Статус: [todo]
   - Сделано: —
   - Осталось: реализация парсинга/валидации/ответов
2) Реестр инструментов
   - Ввести `ToolRegistry`, `Tool`, `ToolInvoker`, метаданные параметров (имя/тип/обязательность/описание/дефолт).
   - Реализовать `tools/list` (полная инвентаризация методов MCP).
   - Статус: [todo]
   - Сделано: —
   - Осталось: определение всех методов и параметров
3) Маппинг MCP методов на ядро (elements/relations)
   - Реализовать: `elements/*`, `relations/*` (create/get/update/delete, list_relations).
   - Делегировать в `core.elements.*`, `core.relations.*`.
   - Статус: [todo]
   - Сделано: —
   - Осталось: все подпункты шага
4) Маппинг MCP методов на ядро (views)
   - Реализовать: list/create/get/delete, get_content, get_image (base64), add_element, add_relation, update_bounds, move_object, remove_object.
   - Делегировать в `core.views.*`.
   - Статус: [todo]
   - Сделано: —
   - Осталось: все подпункты шага
5) Маппинг MCP методов на ядро (folders/search/model)
   - Реализовать: `folders/list`, `folders/ensure`, `search`, `model/save`.
   - Делегировать в `core.folders.*`, `core.search.*`, `core.*` (save).
   - Статус: [todo]
   - Сделано: —
   - Осталось: все подпункты шага
6) Ошибки и валидация
   - Единый маппинг исключений ядра в JSON‑RPC коды; детализированное сообщение + `data` при необходимости.
   - Валидация параметров: типы/обязательность/дефолты на входе MCP.
   - Статус: [todo]
   - Сделано: —
   - Осталось: реализация и тесты негативных веток
7) Тесты [[memory:5587551]]
   - Юнит: парсинг одиночного/батч, `tools/list`, happy‑path по нескольким методам, негативные ветки (unknown method, invalid params).
   - Интеграция (WSL): curl POST на `/mcp/` для ключевых методов; изображение — сохранение из `data_base64` и проверка заголовков.
   - Статус: [todo]
   - Сделано: —
   - Осталось: покрыть критические сценарии
8) Документация
   - `AGENTS.md`: раздел MCP (endpoint, формат, коды ошибок, base64 для бинарных данных, паритет методов).
   - `README.md`: пример `.cursor/mcp.json` с `url`, примеры запросов/ответов.
   - Статус: [todo]
   - Сделано: —
   - Осталось: тексты и примеры

## Ведение прогресса
- Для каждого шага выше поддерживать блоки: Статус/Сделано/Осталось, обновляя по мере выполнения.

## DoD
- `tools/list` перечисляет полный набор MCP‑методов, покрывающих все возможности REST/OpenAPI.
- Все MCP методы успешно вызывают ядро и возвращают структуры, эквивалентные REST.
- Бинарные ответы (PNG/SVG) отдаются в base64 с корректным `content_type`.
- Тесты зелёные; smoke‑проверки проходят; документация обновлена.

## Риски/роллбэк
- Риски: расхождение контрактов (REST vs MCP), неполный паритет — смягчается DoD и тестами; тайминги UI‑треда.
- Роллбэк: отключить `/mcp/` контекст и классы MCP; REST остаётся без изменений.


 