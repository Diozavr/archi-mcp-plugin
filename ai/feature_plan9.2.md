### План: MCP Tool Registry → batch API parity (feature-plan 9.2)

## Цель
- Перевести MCP-интерфейс (ToolRegistry) на пакетные операции и привести его к полному соответствию с актуальным REST-контрактом (`resources/openapi.json`).
- Убрать устаревшие (single) инструменты; оставить только batch-варианты и новые пути для операций с видами.

## Контекст/Предпосылки
- OpenAPI v9.1/9.2: `/elements` и `/relations` — полностью пакетные GET/POST/PATCH/DELETE; операции с объектами вида — без `{objectId}` в path, пакетные PATCH/DELETE.
- Лимит размера массивов — `maxItems: 50`.
- В ядре уже есть пакетные методы (`ElementsCore.createElements(...)`, `ViewsCore.addElements(...)`, и т.д.).

## Инварианты
- Локальный сервер, только `127.0.0.1`.
- Все операции с EMF/SWT — через `Display.getDefault().syncExec(...)` в core-слое (как сейчас).
- Источник правды по контракту — `com.archimatetool.mcp/resources/openapi.json`.
- batch-лимит: не более 50 элементов на вызов (валидировать в ToolRegistry).

## Целевая архитектура

### Соответствие MCP-инструментов REST-эндпоинтам (без обратной совместимости)
- Элементы:
  - `get_elements(ids[], include_relations?, include_elements?)` → GET `/elements?ids=...` (<=50)
  - `create_elements(items[])` → POST `/elements` (<=50)
  - `update_elements(items[])` → PATCH `/elements` (<=50)
  - `delete_elements(ids[])` → DELETE `/elements` с телом `[ {id} ]` (<=50)
- Отношения:
  - `get_relations(ids[])` → GET `/relations?ids=...` (<=50)
  - `create_relations(items[])` → POST `/relations` (<=50)
  - `update_relations(items[])` → PATCH `/relations` (<=50)
  - `delete_relations(ids[])` → DELETE `/relations` с телом `[ {id} ]` (<=50)
- Виды:
  - `list_views()` → GET `/views`
  - `create_view(type, name)` → POST `/views`
  - `get_view(view_id)` → GET `/views/{id}`
  - `delete_view(view_id)` → DELETE `/views/{id}`
  - `get_view_content(view_id)` → GET `/views/{id}/content`
  - `get_view_image(view_id, format?, scale?, dpi?, bg?, margin?)` → GET `/views/{id}/image`
  - `add_elements_to_view(view_id, items[])` → POST `/views/{id}/add-element` (<=50)
  - `add_relations_to_view(view_id, items[])` → POST `/views/{id}/add-relation` (<=50)
  - `update_objects_bounds(view_id, items[])` → PATCH `/views/{id}/objects/bounds` (<=50)
  - `move_objects_to_container(view_id, items[])` → PATCH `/views/{id}/objects/move` (<=50)
  - `remove_objects_from_view(view_id, object_ids[])` → DELETE `/views/{id}/objects` (<=50)
- Папки/типы/поиск/модель:
  - `types()` → GET `/types`
  - `folders()` → GET `/folders`
  - `folder_ensure(path)` → POST `/folder/ensure`
  - `search(q?, kind?, element_type?, relation_type?, property?[], include_docs?, include_props?, limit?, offset?, debug?, log?)` → GET `/search`
  - `save_model(model_id?, create_backup?)` → POST `/model/save`

### Дизайн параметров MCP (ToolParam)
- Списки ids/items — тип `array`, максимальный размер 50; валидация и человеко-читаемая ошибка при превышении.
- Нейминг в змеиный формат для MCP (например, `view_id`, `object_ids`, `include_relations`).
- items — объекты с полями как в OpenAPI (camelCase), преобразование в core DTO делаем в реализации ToolRegistry.

## Ведение прогресса по шагам 
- Для каждого шага поддерживать чек‑лист:
  - Статус: [done|partial|todo]
  - Сделано: краткий перечень завершённых подпунктов
  - Осталось: оставшиеся подпункты/блокеры/владелец (если есть)
- Если шаг выполнен, агент сразу переходит к следующему до полного завершения плана.

## Шаги реализации
⚠️ Важно: все шаги должны выполняться **последовательно и без остановок**, пока не завершится весь план. Если после выполнения одного шага остаются следующие, агент обязан перейти к ним автоматически, без ожидания подтверждения пользователя.
1) Чистка ToolRegistry от single-инструментов
   - Удалить/заменить: `create_element`, `add_element_to_view`, и пр. одиночные операции.
   - Оставить/добавить только перечисленные batch-инструменты (см. список выше).
   - Статус: [todo]

2) Добавить новые MCP-инструменты
   - Элементы: `get_elements`, `create_elements`, `update_elements`, `delete_elements`.
   - Отношения: `get_relations`, `create_relations`, `update_relations`, `delete_relations`.
   - Виды: `get_view`, `delete_view`, `add_elements_to_view`, `add_relations_to_view`, `update_objects_bounds`, `move_objects_to_container`, `remove_objects_from_view`.
   - Для массивов — валидация `<=50`.
   - Статус: [todo]

3) Маппинг параметров → core DTO
   - Преобразование имен MCP-параметров в поля DTO и сборка команд/квери.
   - В частности: сборка `AddElementsToViewCmd`/`AddRelationsToViewCmd`, `UpdateViewObjectsBoundsCmd`, `MoveViewObjectsCmd` из массивов items.
   - Статус: [todo]

4) Обработка ошибок
   - При превышении лимита — BadRequestException с пояснением.
   - Проброс CoreException через существующий обработчик HTTP → JSON-RPC слой.
   - Статус: [todo]

5) Обновить документацию MCP-инструментов
   - Описание параметров в `ToolParam` (описания, примеры для ids/items, лимиты).
   - Статус: [todo]

6) Тесты
   - Юнит/интеграция: Smoke (Python) уже покрывает batch REST-флоу; добавить быстрые проверки MCP-инструментов через JSON-RPC (локально в Archi).
   - Минимум: create/list/delete для элементов/отношений; добавление на вид; bounds/move; удаление объектов вида.
   - Статус: [todo]

## DoD (Definition of Done)
- ToolRegistry предоставляет MCP-инструменты, полностью покрывающие REST-возможности, определённые в `openapi.json`.
- Все массивные операции имеют лимит 50 и валидируются.
- Одиночные инструменты удалены; клиентам доступна только batch-семантика.
- Базовые тесты проходят: Python smoke — зелёный; JSON-RPC быстрые ручные — без ошибок.

## Риски/роллбэк
- Риск: клиенты, использующие старые single-инструменты, перестанут работать — требуется синхронизация.
- Риск: несоответствие полей items/DTO — маппинг и валидация.
- Роллбэк: восстановить прежние инструменты из истории и/или добавить шим-обёртки (не рекомендуется).

## Примечания по реализации (подсказки)
- Лимит массивов: утилитарный метод `validateArraySize(params.get("ids"), 50)`.
- При формировании `items` из параметров MCP — проверять и нормализовать типы (числа/строки) перед передачей в core.
- Для `remove_objects_from_view` допускается возврат агрегата `{total, deleted}` в стиле REST, либо пустой `{ok:true}` — на усмотрение, но лучше возвращать агрегат.


