### План рефакторинга: общий ядровой слой для REST и MCP (feature-plan 5.1)

# Цель:
- Выделить из `*HttpHandler` всю бизнес-логику работы с моделью (Archi/EMF/SWT) в общий ядровой слой.
- Оставить в `*HttpHandler` только разбор протокола (HTTP/JSON) и маппинг ошибок/DTO.
- Подготовить основу для второго интерфейса — MCP (JSON‑RPC), переиспользуя те же ядровые методы.

—

# Инварианты:
- Локальный сервер, только `127.0.0.1`; порт — как в `Config.resolvePort()`.
- Все изменения EMF выполняются в UI‑треде: `Display.getDefault().syncExec(...)`.
- Источник правды контракта REST — `resources/openapi.json`.
- Тесты — в `com.archimatetool.mcp/test` [[memory:5587551]].

—

# Целевая архитектура (слои):
- Протокольные адаптеры:
  - REST: тонкие `HttpHandler` (только парсинг входа/выхода, HTTP‑коды, заголовки).
  - MCP: `JsonRpcHttpHandler` (JSON‑RPC 2.0), маппинг ошибок в error‑объекты.
- Ядро (application layer):
  - Команды/кейсы использования (методы) с чёткими, типизированными параметрами и возвратом DTO/результатов.
  - Валидации параметров, единые исключения.
  - Потоковая политика (UI‑тред) инкапсулирована в ядре, а не в адаптерах.
- Инфраструктура/доступ к модели:
  - Переиспользуем `ActiveModelService`, `ElementService`, `RelationService`, `ViewService` и утилиты `ModelApi` (DTO‑маппинг).

## Предлагаемые пакеты/классы:
- `com.archimatetool.mcp.core` — корневой пакет ядра.
  - `core.elements.ElementsCore` — create/get/update/delete, listRelations.
  - `core.relations.RelationsCore` — create/get/update/delete.
  - `core.views.ViewsCore` — list/create/get/delete; content; image; addElement; addRelation; updateBounds; move; removeObject.
  - `core.search.SearchCore` — поиск (параметры — типизированные).
  - `core.folders.FoldersCore` — дерево/ensure.
  - `core.errors.CoreException` + подтипы: `BadRequestException`, `NotFoundException`, `ConflictException`, `UnprocessableException`.
  - `core.validation.*` — валидаторы параметров (ID, типы, границы, policy и т. п.).
  - `core.types.*` — типизированные входные структуры (команды) для методов ядра.

## Маппинг REST → ядро (примеры):
- `POST /elements` → `ElementsCore.createElement(CreateElementCmd)`.
- `GET /elements/{id}` → `ElementsCore.getElement(GetElementQuery)` (+ включения `relations`).
- `GET /elements/{id}/relations` → `ElementsCore.listRelations(ListElementRelationsQuery)`.
- `PATCH /elements/{id}` → `ElementsCore.updateElement(UpdateElementCmd)`.
- `DELETE /elements/{id}` → `ElementsCore.deleteElement(DeleteElementCmd)`.
- `POST /relations` → `RelationsCore.createRelation(CreateRelationCmd)` ... и т. д. для views/search/folders/model/save.

## Политика ошибок (единая):
- Ядро бросает `CoreException`‑подтипы. Протоколы маппят их в свой формат:
  - REST: HTTP‑коды 400/404/409/422 + JSON‑тело `{ "error": "..." }`.
  - MCP: JSON‑RPC `error` (`code`, `message`, `data`).
- Все остальные исключения — как 500 (REST) / `-32603` (RPC Internal error), с безопасным текстом.

## Потоковая модель:
- Внутри ядра: любые мутации EMF — через `syncExec`. Чтение — без UI‑треда, если безопасно.
- Адаптеры не содержат `syncExec`.

## Валидация/парсинг:
- REST: сохраняем `JsonReader`/`QueryParams` для разбора JSON/Query → затем формируем типизированные `*Cmd/*Query` и зовём ядро.
- MCP: напрямую формируем `*Cmd/*Query` из `params`.

-

# Шаги реализации:
1) Подготовка инфраструктуры ядра
   - Создать пакеты `core/*`, ввести `CoreException` и маппер ошибок для REST.
   - Вынести общие валидации (ID, policy, bounds) в `core.validation`.
   - Статус: [done]
   - Сделано: `core.errors.*`, `ResponseUtil.handleCoreException`, базовый валидатор, проверки `requireNonNull/requireNonEmpty/requireNonNegative`
   - Осталось: —
2) Elements/Relations: миграция
   - Реализовать `ElementsCore`, `RelationsCore` поверх `ServiceRegistry`/`ModelApi`.
   - Обновить `ElementsHttpHandler`, `ElementItemHttpHandler`, `RelationsHttpHandler`, `RelationItemHttpHandler` на делегирование в ядро.
   - Сохранить прежние ответы/коды.
   - Статус: [done]
   - Сделано: `ElementsCore.create/get/update/delete/listRelations`, делегирование из `ElementsHttpHandler` и `ElementItemHttpHandler`; `RelationsCore.create/get/update/delete`, делегирование из `RelationsHttpHandler` и `RelationItemHttpHandler`; негативные тесты валидаций ядра
   - Осталось: —
3) Views: миграция
   - Реализовать `ViewsCore` (включая add‑element, add‑relation, bounds/move/remove, content, image).
   - Перевести `ViewsHttpHandler`, `ViewItemHttpHandler`, `Legacy*` на ядро.
   - Статус: [done]
   - Сделано: `ViewsCore.list/create/get/delete`, `getViewContent`, `addElement`, `addRelation`, операции objects/* (bounds/move/remove), `getViewImage`; делегирование из `ViewsHttpHandler`, `ViewItemHttpHandler`, `LegacyViewContent*`, `LegacyViewAddElement*`
   - Осталось: —
4) Search/Folders/Save: миграция
   - `SearchCore`, `FoldersCore`, `ModelCore` (можно как методы в соответствующих классах).
   - Перевести хендлеры.
   - Статус: [done]
   - Сделано: `SearchCore.search`, `FoldersCore.listFolders/ensureFolder`, `ModelCore.saveModel`; делегирование из `SearchHttpHandler`, `FoldersHttpHandler`, `FolderEnsureHttpHandler`, `ModelSaveHttpHandler`
   - Осталось: —
5) MCP контроллер (подготовка)
   - Ввести `JsonRpcHttpHandler` (контур, без полного набора методов) — делегация в ядро.
   - Список методов MCP строится на базе тех же `*Cmd/*Query` и DTO.
   - Статус: [partial]
   - Сделано: добавлен `JsonRpcHttpHandler` с методами `elements.create`, `elements.get`, `model.save`
   - Осталось: реализовать остальные методы MCP
6) Тесты [[memory:5587551]]
   - Юнит‑тесты ядра: сценарии happy‑path и негативные (исключения/валидации).
   - Тесты REST‑хендлеров — тонкие (проверка маппинга кодов и формата ответа).
   - Интеграционные smoke — без изменений (должны оставаться зелёными).
   - Статус: [todo]
   - Сделано: —
   - Осталось: все подпункты шага
7) Очистка/рефакторинг
   - Удалить дубли логики из хендлеров, оставить только адаптацию.
   - Обновить `AGENTS.md` (архитектура, слой ядра, политика ошибок).
   - Статус: [todo]
   - Сделано: —
   - Осталось: все подпункты шага
   
-

# Ведение прогресса по шагам:
- Для каждого шага выше вести чек‑лист статуса в этом файле:
  - [done] — выполнено полностью;
  - [partial] — частично выполнено: перечислить, что сделано и что осталось;
  - [todo] — не начато.
- Шаблон отметок (поддерживать внизу шага):
  - Статус: [done|partial|todo]
  - Сделано: краткий список подпунктов
  - Осталось: краткий список подпунктов с владельцем/блокерами при наличии
- Пример:
    - Шаг 2 (Elements/Relations):
      - Статус: [partial]
      - Сделано: `ElementsCore.create/get/update/delete/listRelations`, делегирование из `ElementsHttpHandler` и `ElementItemHttpHandler`; `RelationsCore.create/get/update/delete`, делегирование из `RelationsHttpHandler` и `RelationItemHttpHandler`
      - Осталось: негативные тесты ядра

# DoD:
- Все REST‑эндпоинты используют общий ядровой слой; поведение и контракты не изменены.
- MCP (JSON‑RPC) базируется на тех же методах ядра (минимум 2–3 метода для демонстрации).
- Тесты ядра и хендлеров зелёные; smoke‑скрипт проходит.

# Риски/роллбэк:
- Риск: регресс по HTTP‑кодам/сообщениям — смягчается e2e‑smoke и тестами хендлеров.
- Роллбэк: вернуть прежние реализации в хендлерах; ядровые классы оставить выключенными.


