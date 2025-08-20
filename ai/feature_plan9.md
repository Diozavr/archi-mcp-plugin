### План внедрения: Batch API и batch-режим MCP (feature-plan 9)

Цель:
- Перевести максимально возможные операции на пакетный (batch) режим для снижения числа round‑trip и повышения производительности.
- Сохранить полную совместимость одиночных методов и обеспечить паритет REST ↔ MCP (методы и семантика совпадают).

—

Инварианты (актуально на проекте):
- Истина REST‑контракта — `archi-mcp-plugin/com.archimatetool.mcp/resources/openapi.json`; все тексты `summary/description` и параметры синхронизированы с MCP‑описаниями.
- MCP‑сервер Python публикует инструменты, чьи параметры/описания должны соответствовать OpenAPI; при изменениях в OpenAPI обязательно обновлять `archi-mcp-server/server.py` (включая `PARAM_DESCRIPTIONS`) и `archi-mcp-server/README.md`.
- Сеть: только `127.0.0.1:<port>`; переменная `ARCHI_API` указывает базовый URL REST.
- Обратная совместимость не обязательна; при этом по возможности сохраняем «дешёвые» алиасы/старые формы.
- Минимизируем количество MCP‑инструментов: расширяем существующие инструмент(ы) опциональными batch‑параметрами там, где это возможно (учитываем ограничение на число инструментов).

—

Дизайн batch API (REST) без новых путей:
- Общие принципы:
  - Тот же путь/метод обслуживает одиночный и пакетный сценарий. Отличие — по форме тела запроса: объект для одиночного, `{ items: [...] }` для пакетного. Там, где тело отсутствовало (GET/DELETE), добавляем аналогичные POST‑варианты на тот же путь.
  - Единый формат пакетного ответа: агрегированные счётчики и помет‑по‑элементу с `ok|error`.
  - Флаги `atomic` и `continueOnError` в теле запроса. При `atomic=true` — либо все операции успешны, либо откат (где применимо).
  - Ограничения: `maxItems` (по умолчанию 100, конфигурируемо), защита от слишком больших тел.
  - Коды ответов: 200/201 для успешной обработки набора, ошибки — per‑item в теле; HTTP‑ошибки — только для фатальных сбоев запроса.
- Схемы компонентов (добавить в `components.schemas`):
  - `BatchItemResult` — `{ index: integer, ok: boolean, status: integer, result?: any, error?: { code: string, message: string } }`.
  - `BatchResponse` — `{ total: integer, succeeded: integer, failed: integer, items: BatchItemResult[] }`.
  - Узкоспециализированные наборы (`BatchCreateElementsRequest`, `BatchPatchElementsRequest` и т.п.) — см. ниже по ресурсам.

—

Расширения существующих REST‑методов (без `/batch` путей):

1) Elements
- POST `/elements`
  - Create (batch): тело — `{ items: [ { type, name, ... }, ... ], atomic?, continueOnError? }` → `200 BatchResponse<Element>`.
- GET `/elements`
  - Multi‑get: query `ids=a&ids=b` или тело POST‑варианта (см. ниже) → `200 BatchResponse<Element|ElementWithRelations>`; поддержать `include=relations`, `includeElements=true`.
- POST `/elements/get`
  - Альтернативный POST‑вариант для multi‑get (там, где query неудобен): `{ ids:[...], include?, includeElements? }` → `200 BatchResponse<...>`.
- PATCH `/elements`
  - Batch‑patch: `{ items: [{ id, name?, type?, folderId?, properties?, documentation? }], atomic?, continueOnError? }` → `200 BatchResponse<Element>`.
- DELETE `/elements`
  - Batch‑delete: тело — `{ ids:[string], atomic?, continueOnError? }` → `200 BatchResponse`.

2) Relations
- POST `/relations` — одиночный и batch‑create как в Elements.
- POST `/relations/get` — multi‑get.
- PATCH `/relations` — batch‑patch.
- DELETE `/relations` — batch‑delete.

3) Views (диаграммные операции)
- POST `/views/get` — `{ ids:[string] }` → `BatchResponse<View>`.
- POST `/views/content` — `{ ids:[string], limit?, offset? }` → `BatchResponse<ViewContent>`.
- POST `/views/{id}/add-element`
  - Одиночный: `{ elementId, parentObjectId?, bounds, style? }` → `{ objectId }`.
  - Batch: `{ items:[{ elementId, parentObjectId?, bounds, style? }, ...], atomic?, continueOnError? }` → `BatchResponse<{objectId:string}>`.
- POST `/views/{id}/add-relation`
  - Одиночный и batch по аналогии (`policy`, `suppressWhenNested`).
- PATCH `/views/{id}/objects/bounds`
  - Одиночный: `{ objectId, x?, y?, w?, h? }` → `ViewObject`.
  - Batch: `{ items:[{ objectId, x?, y?, w?, h? }], atomic?, continueOnError? }` → `BatchResponse<ViewObject>`.
- PATCH `/views/{id}/objects/move`
  - Одиночный: `{ objectId, parentObjectId, bounds?, keepExistingConnection? }` → `ViewObject`.
  - Batch: `{ items:[{ objectId, parentObjectId, bounds?, keepExistingConnection? }], ... }` → `BatchResponse<ViewObject>`.
- POST `/views/{id}/objects/remove`
  - `{ objectIds:[string], atomic?, continueOnError? }` → `BatchResponse`.

4) Folders
- POST `/folder/ensure`
  - Batch: `{ paths:[string] }` → `BatchResponse<Folder>`.


MCP (внутри плагина и Python) — паритет без увеличения числа инструментов и множественные имена методов:
- Переименовать инструменты в множественное число, каждый поддерживает одиночный и пакетный режимы через `ids`/`items`:
  - Elements: `get_elements`, `create_elements`, `update_elements`, `delete_elements`.
  - Relations: `get_relations`, `create_relations`, `update_relations`, `delete_relations`.
  - Views (метаданные/контент): `get_views`, `get_views_content`.
  - Операции на видах: `add_elements_to_view`, `add_relations_to_view`, `update_objects_bounds`, `move_objects_to_container`, `remove_objects_from_view`.
  - Справочные: `types`, `folders`, `folder_ensure` (поддержать массив путей через `paths`).
- В обеих реализациях MCP (Java внутри плагина и Python `archi-mcp-server/server.py`) заменить старые имена на перечисленные, обратная совместимость не требуется.
- Схемы MCP: для новых параметров указать `Annotated[..., Field(description=...)]`, обновить `PARAM_DESCRIPTIONS` и описания; возвращаемые структуры: одиночный вызов → одиночный DTO, пакетный → `BatchResponse`.

—

Шаги реализации (итерациями):
1) Спецификация
- Обновить OpenAPI: расширить существующие пути для работы с массивами (`items`/`ids`) и добавить компоненты (`BatchItemResult`, `BatchResponse`, request‑схемы). Краткие и понятные `summary/description`.
- Отразить лимиты (`maxItems`, допустимые размеры), описать `atomic/continueOnError`.

2) Ядро/утилиты
- Общая утилита выполнения батчей: `BatchExecutor.execute(items, atomic, continueOnError, Function)` с развёрнутой диагностикой и маппингом исключений в статус/код.
- Минимизировать количество `UiExec.sync(...)` — по возможности группировать однотипные изменения в один UI‑проход.

3) Core/Services
- ElementsCore/RelationsCore/ViewsCore: ввести методы `batchCreate`, `batchGet`, `batchPatch`, `batchDelete` и спец‑батчи для объектов вида.
- Валидации: предобработка входных массивов (пустые, дубликаты ID, отсутствующие объекты).

4) HTTP‑хендлеры
- Обновить существующие хендлеры: принимать как одиночные объекты, так и `{items:[...]}`; для массивов отвечать `BatchResponse`.
- Для массовых GET/DELETE добавить POST‑варианты (`/elements/get`, `/relations/get`, а также `DELETE /elements|relations` с телом) во избежание ограничений на тело у GET/DELETE.

5) MCP (внутри плагина и Python)
- Переименовать инструменты в обоих MCP в множественное число; обновить регистрацию/хендлеры.
- Расширить параметры (`ids`, `items`) согласно плану; собрать ответы в `BatchResponse`.
- Обновить `PARAM_DESCRIPTIONS`, описания инструментов и `README.md` (маппинг MCP → REST) в обоих репозиториях.

6) Тесты
- Unit (Java): batch‑валидации, маппинг ошибок, `atomic`/`continueOnError` сценарии.
- Smoke (WSL):
  - `POST /elements` с `{items:[...]}` → 3 элемента, 1 с ошибкой, `continueOnError=true`.
  - `PATCH /views/{id}/objects/bounds` с `{items:[...]}` для нескольких объектов.
  - `POST /folder/ensure` с `{paths:[...]}` для нескольких путей.
- Python MCP: несколько вызовов инструментов с `ids/items` и проверка формы ответа.

7) Документация
- Обновить `AGENTS.md` (напоминание о синхронизации описаний, ссылка на OpenAPI), `README.md` плагина (примеры batch‑вызовов), `archi-mcp-server/README.md` (маппинг и примеры), `server.py` описания параметров. В примерах использовать те же пути без `/batch`.

8) Нефункциональные требования
- Лимиты: `maxItems`, таймауты, ограничения на суммарный размер тела и (при введении массового рендера) на `views/image`.
- Отчётность: счётчики времени и количества операций в DEBUG‑логах.

—

Совместимость и миграция
- Одиночные REST‑пути и MCP‑вызовы продолжают работать (одиночный формат тела/параметров).
- Новые batch‑параметры являются дополняющими; при их отсутствии поведение тождественно текущему.
- Для клиентов рекомендуется миграция на batch при массовых сценариях.

—

Критерии готовности (DoD)
- OpenAPI обновлён: существующие пути поддерживают batch‑режим; схемы добавлены; валидатор проходит.
- Реализованы batch‑режимы на существующих путях; одиночные контракты сохранены.
- Оба MCP обновлены: новые множественные имена, расширенные параметры, паритет по методам и семантике ответов.
- Тесты (unit + smoke) зелёные; примеры в README воспроизводимы.

—

Роллбэк
- Откатить обработку массивов в хендлерах/сервисах/OpenAPI; вернуть одиночные формы тел/ответов.
- Вернуть старые имена MCP‑инструментов при необходимости.
- Документацию привести к состоянию до feature‑plan 9.

—

Приложение A. Быстрые примеры (WSL)
```bash
# Создание трёх элементов батчем (в т.ч. один с ошибкой типа)
curl -sS -X POST http://127.0.0.1:8765/elements \
  -H 'Content-Type: application/json' \
  -d '{
    "items": [
      {"type":"business-actor","name":"Actor A"},
      {"type":"business-role","name":"Role B"},
      {"type":"unknown-type","name":"Oops"}
    ],
    "continueOnError": true
  }' | jq .

# Массовое обновление координат объектов вида
curl -sS -X PATCH http://127.0.0.1:8765/views/$VID/objects/bounds \
  -H 'Content-Type: application/json' \
  -d '{"items":[{"objectId":"$OID1","x":10},{"objectId":"$OID2","y":20}]}' | jq .
```


