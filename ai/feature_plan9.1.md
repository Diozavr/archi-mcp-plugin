### План: batch-запись без новых эндпоинтов (feature-plan 9.1)

## Цель
- Повысить производительность за счёт перевода ключевых write-операций на работу с массивами элементов/объектов без введения новых путей.
- Изменить текущие операции так, чтобы они принимали массивы входных данных и возвращали массивы результатов.
- Обратная совместимость не требуется.

## Контекст/Предпосылки
- GET-операции уже поддерживают списки, проблема — в множественных изменениях модели и видов.
- Обновлять необходимо и `resources/openapi.json`, и реализацию MCP (общая реализация в `core.*`).
- Хэндлеры REST находятся в `http.handlers.*` и пользуются `core.*` и `core.types.*` DTO.

## Инварианты
- Локальный сервер, только `127.0.0.1`; порт — как в `Config.resolvePort()`.
- Операции с EMF/SWT — через `Display.getDefault().syncExec(...)`.
- Источник правды REST‑контракта — `com.archimatetool.mcp/resources/openapi.json`.
- Тесты — в `com.archimatetool.mcp/test`.

## Целевая архитектура 

### Аспект архитектуры 1 — REST контракт (OpenAPI)
- Элементы (`/elements`) — полностью пакетные операции; пути с `{id}` убираются:
  - `GET /elements?ids=a&ids=b` — массив id в query, опционально `include=relations`, `includeElements=true`; ответ — массив `Element | ElementWithRelations`.
  - `POST /elements` — тело: массив `{type,name,folderId?,properties?,documentation?}`; ответ — массив `Element`.
  - `PATCH /elements` — тело: массив `{id,name?,type?,folderId?,properties?,documentation?}`; ответ — массив `Element`.
  - `DELETE /elements` — тело: массив `{id}`; ответ — `200` с `{ total, deleted }`.
- Отношения (`/relations`) — полностью пакетные операции; пути с `{id}` убираются:
  - `GET /relations?ids=a&ids=b` — массив id в query; ответ — массив `Relation`.
  - `POST /relations` — тело: массив `{type,name?,sourceId,targetId,folderId?,properties?,documentation?}`; ответ — массив `Relation`.
  - `PATCH /relations` — тело: массив `{id,name?,type?,properties?,documentation?}`; ответ — массив `Relation`.
  - `DELETE /relations` — тело: массив `{id}`; ответ — `200` с `{ total, deleted }`.
- Операции с объектами вида — без `{objectId}` в path, пакетно:
  - `POST /views/{id}/add-element` — массив `{elementId,parentObjectId?,bounds|x,y,w,h,style?}` → массив `{objectId}`.
  - `POST /views/{id}/add-relation` — массив `{relationId,sourceObjectId?,targetObjectId?,policy?,suppressWhenNested?}` → массив `Connection | {suppressed:true}`.
  - `PATCH /views/{id}/objects/bounds` — массив `{objectId,x?,y?,w?,h?}` → массив `ViewObject`.
  - `PATCH /views/{id}/objects/move` — массив `{objectId,parentObjectId,bounds?,keepExistingConnection?}` → массив `ViewObject`.
  - `DELETE /views/{id}/objects` — массив `{objectId}` → `200` `{ total, deleted }`.
- Обратная совместимость не требуется; одиночные схемы и пути удаляются.
- Лимит на размер всех пакетных массивов (включая query `ids`) — максимум 50 элементов.

### Аспект архитектуры 2 — DTO/типы команд (`core.types.*`)
- Ввести «пакетные» команды, сохранив названия областей ответственности:
  - `CreateElementItem`, `CreateElementsCmd { List<CreateElementItem> items }`.
  - `CreateRelationItem`, `CreateRelationsCmd { List<CreateRelationItem> items }`.
  - `AddElementToViewItem`, `AddElementsToViewCmd { String viewId; List<AddElementToViewItem> items }`.
  - `AddRelationToViewItem`, `AddRelationsToViewCmd { String viewId; List<AddRelationToViewItem> items }`.
  - `UpdateViewObjectBoundsItem`, `UpdateViewObjectsBoundsCmd { String viewId; List<UpdateViewObjectBoundsItem> items }`.
  - `MoveViewObjectItem`, `MoveViewObjectsCmd { String viewId; List<MoveViewObjectItem> items }`.
  - `DeleteViewObjectItem`, `DeleteViewObjectsCmd { String viewId; List<DeleteViewObjectItem> items }`.
- Старые одиночные DTO можно удалить/заменить (совместимость не требуется).

### Аспект архитектуры 3 — Core (`core.elements.*`, `core.relations.*`, `core.views.*`)
- Переход на массивы в сигнатурах и реализациях:
  - `ElementsCore.createElements(CreateElementsCmd)` → `List<Map<String,Object>>`.
  - `RelationsCore.createRelations(CreateRelationsCmd)` → `List<Map<String,Object>>`.
  - `ViewsCore.addElements(AddElementsToViewCmd)` → `List<Map<String,Object>>`.
  - `ViewsCore.addRelations(AddRelationsToViewCmd)` → `List<Map<String,Object>>` (смешанные per-item: `Connection` или `{suppressed:true}`).
  - `ViewsCore.updateBounds(UpdateViewObjectsBoundsCmd)` → `List<Map<String,Object>>`.
  - `ViewsCore.moveObjects(MoveViewObjectsCmd)` → `List<Map<String,Object>>`.
  - `ViewsCore.deleteObjects(DeleteViewObjectsCmd)` → `{ total, deleted }`.
- Валидация — на уровне элемента массива; ошибки бросать как `CoreException` с указанием индекса или аккумулировать как частичные результаты (решение ниже).

### Аспект архитектуры 4 — HTTP‑хендлеры (`http.handlers.*`)
- `ElementsHttpHandler`:
  - `GET /elements?ids=` — чтение массива id из query; возврат массива DTO (с поддержкой `include`, `includeElements`).
  - `POST /elements`, `PATCH /elements`, `DELETE /elements` — чтение тела как массива; возврат массивов/агрегата.
- `RelationsHttpHandler`:
  - `GET /relations?ids=`, `POST /relations`, `PATCH /relations`, `DELETE /relations` — пакетные операции аналогично элементам.
- `ViewItemHttpHandler`:
  - `POST /views/{id}/add-element`, `POST /views/{id}/add-relation` — тело: массив; ответ: массив, код 200.
  - `PATCH /views/{id}/objects/bounds`, `PATCH /views/{id}/objects/move` — без `{objectId}` в path; тело: массив с `objectId`.
  - `DELETE /views/{id}/objects` — тело: массив `{objectId}`; ответ: `200` `{ total, deleted }`.

### Аспект архитектуры 5 — JSON утилиты
- Расширить `JsonReader` для массивов:
  - `boolean isArrayRoot()`; `int arraySize()`; `JsonReader at(int idx)`;
  - `List<JsonReader> optArray(String key)` для случая объектов с вложенным `items` (если понадобится).
- Позволит хендлерам единообразно обрабатывать как массив‑root, так и объект с массивом.

## Шаги реализации
1) Обновить OpenAPI
   - Завершить правки в `resources/openapi_v91.json` по текущему черновику.
   - Переименовать (заменить) на основной `resources/openapi.json`.
   - Включить: пакетные `GET/POST/PATCH/DELETE` для `/elements`, `/relations`; новые пути для объектов вида без `{objectId}`.
   - Коды ответов: для пакетных write — `200`; для удаления объектов — `200` с агрегатом.
   - Для всех массивов запроса и для query `ids` указать `maxItems: 50`.
   - Статус: [done]
   - Сделано: `openapi_v91.json` переименован в `openapi.json`.
   - Осталось: —

2) Расширить `JsonReader` массивными методами
   - Добавить API для чтения корня‑массива и получения элементов.
   - Нужны модульные тесты.
   - Статус: [done]
   - Сделано: добавлены методы `isArrayRoot/arraySize/at/optArray` и тесты
   - Осталось: —

3) Ввести пакетные DTO в `core.types.*`
  - Создать `*Item` и `*Cmd` с `List<...>`.
  - Заменить использования одиночных DTO в core/handlers.
  - Статус: [done]
  - Сделано: добавлены `*Item` и `*Cmd` классы для пакетных операций; core и хендлеры переведены на новые команды
  - Осталось: —

4) Изменить Core: переход на массивы
  - `ElementsCore`, `RelationsCore`, `ViewsCore`: заменить методы на пакетные версии; реализовать итерацию по элементам.
  - Решение по ошибкам: fail‑fast на первой ошибке (проще) ИЛИ частичные результаты. Для минимизации трудозатрат — fail‑fast (ошибка прерывает весь батч).
  - Статус: [done]
  - Сделано: реализованы пакетные методы и базовые тесты; выбран подход fail‑fast
  - Осталось: —

5) Обновить HTTP‑хендлеры
  - `ElementsHttpHandler`, `RelationsHttpHandler`, `ViewItemHttpHandler` — парсинг массивов, вызов пакетных Core‑методов, форматирование ответов.
  - Статус: [partial]
  - Сделано: элементы, связи и объекты вида обрабатывают массивы
  - Осталось: ручная проверка через curl (curl http://127.0.0.1:8765/status → connection refused)

6) Тестирование
  - Юнит‑тесты на core: создание элементов/отношений батчем; операции на видах батчем; негативные сценарии (not found, conflict, bad request).
  - Обновить `test/test_smoke.sh` под пакетный контракт. Мини‑чеклист для smoke:
     - Предусловие: активная модель (например, `Archisurance.archimate`).
     - `POST /elements` — создать 5 элементов одним запросом; проверить длину ответа и наличие `id`.
     - `GET /elements?ids=` — запросить те же 5 `id`; проверить длину и соответствие `name`.
     - `PATCH /elements` — переименовать 3 из 5; проверить изменения.
     - `POST /relations` — создать 4 связи одним запросом; проверить `id` и `sourceId/targetId`.
     - `GET /relations?ids=` — проверить возврат 4 связей.
     - `PATCH /relations` — обновить `name` у 2 связей; проверить изменения.
     - `POST /views` — создать вид; получить `id` вида.
     - `POST /views/{id}/add-element` — добавить 5 элементов на вид батчем с `bounds`; убедиться в 5 `objectId`.
     - `POST /views/{id}/add-relation` — добавить 4 соединения батчем; убедиться в 4 результатах (или `suppressed` где применимо).
     - `PATCH /views/{id}/objects/bounds` — изменить bounds у 3 объектов батчем; проверить, что координаты применены.
     - `PATCH /views/{id}/objects/move` — переместить 2 объекта в контейнер; проверить `parentObjectId`.
     - `DELETE /views/{id}/objects` — удалить 5 объектов батчем; проверить `{total,deleted}`.
     - `DELETE /relations` и `DELETE /elements` — удалить созданные сущности батчем; сверить `{total,deleted}`.
     - `POST /model/save` — проверить `saved:true`.
  - Статус: [partial]
  - Сделано: добавлены юнит‑тесты и обновлён smoke‑скрипт под пакетный контракт
   - Осталось: выполнить `mvn -q -f com.archimatetool.mcp/pom.xml test` (PluginResolutionException, Network is unreachable) и `bash test_smoke.sh` (Server not reachable)

7) Синхронизация описаний для LLM
  - Обновить описания параметров MCP‑инструментов в `archi-mcp-server/server.py` (раздел `PARAM_DESCRIPTIONS`). Источник правды — OpenAPI.
  - Статус: [todo]
  - Сделано: —
  - Осталось: синхронизировать параметрические описания; файл `archi-mcp-server/server.py` в репозитории не найден

## DoD (Definition of Done)
- Сборка успешна, тесты зелёные.
- OpenAPI валиден, Swagger UI группирует по тегам и показывает массивные схемы.
- Все перечисленные write‑операции принимают массив и возвращают массив (или агрегированный результат для delete).
- Smoke‑скрипт успешно создаёт/правит/удаляет десятки объектов за один запрос.

## Тестирование
- Юнит‑тесты: покрыть пакетные методы в `ElementsCore`, `RelationsCore`, `ViewsCore` на успех/ошибки.
- Интеграционные: обновить `test_smoke.sh`: массовое создание 10+ элементов, 10+ связей, добавление на вид, правка bounds и move, удаление объектов вида.

## Риски/роллбэк
- Риски:
  - Большие массивы → рост времени блока UI (syncExec). Митигация: рекомендовать разумные размеры батчей (например, ≤500 объектов).
  - Fail‑fast прерывает весь батч. Альтернатива — частичные результаты (возможна в будущем).
  - Смена контрактов без обратной совместимости потребует синхронизации клиентов.
- Роллбэк:
  - Вернуть одиночные DTO/эндпоинты из предыдущей версии `openapi.json` и реализаций core/handlers.

## Ведение прогресса по шагам
- 1) OpenAPI: Статус [done] — Сделано: заменён `openapi_v91.json` на `openapi.json` — Осталось: —
- 2) JsonReader: Статус [done] — Сделано: добавлены методы массивов и тесты — Осталось: —
- 3) DTO: Статус [done] — Сделано: добавлены пакетные *Item/*Cmd, core и handlers переведены на новые команды — Осталось: —
- 4) Core: Статус [done] — Сделано: реализованы пакетные методы в ElementsCore/RelationsCore/ViewsCore и добавлены юнит-тесты — Осталось: —
  - 5) Handlers: Статус [partial] — Сделано: элементы, связи и объекты вида обрабатывают массивы — Осталось: ручная проверка через curl (curl http://127.0.0.1:8765/status → connection refused).
  - 6) Tests/smoke: Статус [partial] — Сделано: добавлены юнит‑тесты для batch core и обновлён smoke‑скрипт — Осталось: `mvn test` (PluginResolutionException: Network is unreachable) и `test_smoke.sh` (Server not reachable).
  - 7) MCP server.py: Статус [todo] — Сделано: — — Осталось: синхронизировать описания параметров (файл `archi-mcp-server/server.py` не найден).


