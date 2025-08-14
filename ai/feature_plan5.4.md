### План внедрения: MCP JSON‑RPC — полный набор методов (feature-plan 5.4)

Цель:
- Довести MCP внутри плагина до паритета с FastMCP+REST: опубликовать все инструменты, доступные через REST, с корректными JSON Schema параметров и стабильными ответами.
- Сохранить обратную совместимость с уже реализованными методами и схемами.

—

Процесс и фиксация прогресса:
- Работать батчами по логическим блокам (например, блок «Elements», затем «Relations» и т.д.).
- По завершении блока — обновлять этот план: проставлять статусы (например, [done]/[partial]/[todo]) у соответствующих шагов и кратко фиксировать «Сделано/Осталось».
- Специально не останавливаться после каждого микрошагa ради обновления плана; вносить изменения пакетно, когда блок завершён и проверен.

—

Базовые требования (актуально):
- Endpoint: `POST /mcp` (JSON‑RPC 2.0).
- Поддержаны: `initialize`, `notifications/initialized`, `tools/list` (объект с массивом `tools`), `tools/call` (`params.arguments` и legacy `params.args`), `prompts/list`, `resources/list`, `logging/setLevel`.
- Схемы параметров: `$schema: draft-07`, `type: object`, `properties`, `required` (только если непусто), `additionalProperties=false`; для `array` — `items` (по умолчанию `{type:string}`), для `object` — `properties:{}, additionalProperties:true`.

—

Шаг 1. Перечень инструментов к добавлению (паритет с FastMCP + REST)
- Элементы:
  - `get_element(element_id: string, include_relations?: boolean, include_elements?: boolean)`
  - `update_element(element_id: string, name?: string, type?: string, folder_id?: string, properties?: object, documentation?: string)`
  - `delete_element(element_id: string)`
- Связи:
  - `create_relation(type: string, source_id: string, target_id: string, name?: string, properties?: object, documentation?: string)`
  - `get_relation(relation_id: string)`
  - `update_relation(relation_id: string, name?: string, type?: string, properties?: object, documentation?: string)`
  - `delete_relation(relation_id: string)`
- Виды:
  - `get_view(view_id: string)`
  - `delete_view(view_id: string)`
  - `add_relation_to_view(view_id: string, relation_id: string, source_object_id?: string, target_object_id?: string, policy?: "auto"|"fail", suppress_when_nested?: boolean)`
  - `update_object_bounds(view_id: string, object_id: string, x?: integer, y?: integer, w?: integer, h?: integer)`
  - `move_object_to_container(view_id: string, object_id: string, parent_object_id: string|"0", bounds?: {x?:int,y?:int,w?:int,h?:int}, keep_existing_connection?: boolean)`
  - `remove_object_from_view(view_id: string, object_id: string)`
- Справочные/прочие (уже есть либо дополняем):
  - `list_views()`, `create_view(type: string, name: string)`, `get_view_content(view_id: string)`, `get_view_image(view_id: string, format?: "png"|"svg", scale?: number, bg?: string, dpi?: integer, margin?: integer)`
  - `types()`, `folders()`, `folder_ensure(path: string)`, `search(...)`, `save_model()`

Шаг 2. Реестр инструментов (`ToolRegistry`)
- Добавить регистрации для всех недостающих инструментов выше. 
- Параметры описывать через `ToolParam` с типами: `string|integer|number|boolean|array|object`.
- Для сложных параметров:
  - `properties` (object) → тип `object` (свободные поля), краткое описание.
  - `bounds` (object) → тип `object` с полями `x,y,w,h` как `integer`, все опциональные для PATCH‑сценариев.
  - `policy` → тип `string`, добавить `enum` в описание (минимум в тексте, при необходимости расширить ToolParam для enum).

Шаг 3. Маппинг в ядро (invoker’ы)
- По возможности использовать существующие core‑классы (пример: `ElementsCore`, `ViewsCore`, `FoldersCore`, `SearchCore`, `ModelCore`).
- Если отсутствует `RelationsCore` — добавить его в отдельной задаче или временно вызывать соответствующие сервисы напрямую (`ServiceRegistry`/`ModelApi`).
- Правила соответствия REST:
  - Возвращаемые DTO должны соответствовать OpenAPI (`resources/openapi.json`).
  - Ошибки маппить в коды JSON‑RPC: `-32004` NotFound, `-32001` BadRequest, `-32009` Conflict, `-32022` Unprocessable (как уже сделано), иначе `-32603`.

Шаг 4. Схемы параметров (строгие, дружественные для GPT)
- Все инструменты должны иметь валидный `inputSchema`:
  - `$schema: draft-07` присутствует.
  - `required` — только если непусто.
  - `array.items` задан.
  - Для `object` — хотя бы пустые `properties:{}` и `additionalProperties:true` (если нет строгой схемы).
- Для полей с альтернативными именами (например, `arguments` vs `args`) — хранить схему под `arguments`, хэндлер уже поддерживает оба.

Шаг 5. Юнит‑тесты
- Расширить `JsonRpcHttpHandlerTest`:
  - `tools/list` — проверить наличие ключевых инструментов (`get_element`, `create_relation`, `add_relation_to_view`, `update_object_bounds`, ...).
  - `tools/call` — негативные кейсы: отсутствующие/неверные параметры.
  - (Опционально) моковые позитивные кейсы с заглушками, не требующие EMF/SWT.

Шаг 6. Интеграционные smoke‑проверки
- В `com.archimatetool.mcp/test/test_smoke.sh` добавить несколько JSON‑RPC вызовов для новых инструментов (минимум: `get_element`, `create_relation`, `add_relation_to_view`, `update_object_bounds`).
- Синхронизировать с REST шагами уже присутствующего сценария.

Шаг 7. Документация
- `README.md`: список доступных MCP инструментов + краткие примеры.
- `AGENTS.md`: указать, что MCP покрывает паритет с REST (ссылки на OpenAPI для описания DTO).
- При обновлении инструментов — не забывать синхронизировать описания и OpenAPI (при необходимости).

Шаг 8. Роллбэк
- Вернуть реестр к минимальному набору (как в 5.3), удалить регистрации дополнительных инструментов.

Критерии готовности (DoD)
- `tools/list` публикует полный набор (≈ 24–26 инструментов), как в FastMCP.
- Все инструменты принимают корректные параметры согласно `inputSchema` и возвращают ожидаемые DTO/коды ошибок.
- Cursor/GPT корректно инициализируются, видят инструменты, в логах нет ошибок валидации схем.
- Файл плана обновлён: у выполненных шагов выставлены статусы и краткие заметки по результатам.


