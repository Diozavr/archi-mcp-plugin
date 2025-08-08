### План внедрения: добавление связи на View через REST + MCP (feature-plan 1)

Цель: реализовать добавление диаграммного соединения (IDiagramModelArchimateConnection) на конкретный View для уже существующей связи модели (IArchimateRelationship), а также опубликовать этот сценарий как MCP‑инструмент.

—

Инварианты (см. `AGENTS.md`):
- Слушаем только `127.0.0.1:8765`.
- Любые изменения EMF‑модели — строго через `Display.getDefault().syncExec(...)`.
- Формат существующих ответов REST не меняем; новые маршруты/поля — обратно совместимы с текущим поведением.
- Источник правды для REST — `resources/openapi.json`. `archi-mcp-server/server.py` — источник описаний для LLM.

Процесс выполнения:
- Идём по шагам 1 → …, каждый шаг минимальный и проверяемый.
- Коммиты атомарные: `feat(view-add-relation-step-01): ...`.

Smoke‑проверки (WSL):
```bash
curl -sS http://127.0.0.1:8765/status | jq .
curl -sS "http://127.0.0.1:8765/views" | jq .
curl -sS "http://127.0.0.1:8765/views/{VIEW_ID}/content" | jq .
```

Критерии готовности фичи:
- Через REST можно создать соединение на виде для существующей `relationId` с явными `sourceObjectId/targetObjectId`.
- Поддержан режим «auto»: при отсутствии неоднозначности объекты подбираются автоматически.
- Появился MCP‑инструмент `add_relation_to_view(...)` с краткими описаниями параметров.
- README/AGENTS (сервер/плагин) обновлены.

—

Шаг 1. Спецификация REST (OpenAPI)
- Цель: формализовать контракт до кода.
- Действия:
  - Добавить маршрут: `POST /views/{viewId}/add-relation`.
  - Тело запроса:
    ```json
    {
      "relationId": "rel-123",
      "sourceObjectId": "obj-aaa (optional)",
      "targetObjectId": "obj-bbb (optional)",
      "policy": "auto|fail"  
    }
    ```
    - `policy=auto` — попытаться автоматически подобрать объекты по концам связи, если на виде по каждому элементу ровно по одному вхождению.
    - `policy=fail` — при отсутствии `sourceObjectId/targetObjectId` вернуть ошибку (без автоподбора).
  - Ответы:
    - 201: DTO соединения (как в `viewContentToDto`: `id`, `relationId`, `sourceObjectId`, `targetObjectId`, опц. `bendpoints`, `label`).
    - 400: отсутствуют обязательные поля (`relationId`), неверные значения `policy`.
    - 404: не найден `viewId`/`relationId`/указанные `sourceObjectId|targetObjectId` или объекты не принадлежат виду.
    - 409: нет активной модели.
    - 422: указанные `sourceObjectId|targetObjectId` не соответствуют концам `relationId`.
  - Обновить `resources/openapi.json` (path, requestBody schema, responses, описания полей).
- Smoke: `jq -r '.paths["/views/{viewId}/add-relation"].post.summary' resources/openapi.json` → понятное краткое описание.
- Критерий приёмки: спецификация оформлена, понятна и консистентна с остальными маршрутами.

Шаг 2. Бэкенд (Java): хендлер `POST /views/{viewId}/add-relation`
- Цель: реализовать логику на сервере плагина.
- Действия:
  - Ввести новый HttpHandler (по текущей архитектуре: в `HttpServerRunner` или вынести в `.../http/handlers/AddRelationToViewHttpHandler.java`).
  - Парсинг JSON тела: использовать имеющийся парсер (если уже добавлен `JsonReader` из refactor‑плана — через него; иначе временно текущий способ без изменения формата).
  - Валидация входа:
    - `relationId` — обязателен.
    - Для `policy`: default = `auto`.
  - Разрешение `viewId` → `IDiagramModel`.
  - Разрешение `relationId` → `IArchimateRelationship`.
  - Режимы выбора объектов на виде:
    - Если оба `sourceObjectId` и `targetObjectId` заданы — проверить их принадлежность `viewId` и соответствие концам связи, иначе 422.
    - Если хотя бы один не задан и `policy=auto` — найти объекты по каждому элементу конца связи; если по каждой стороне ровно один объект — использовать его, иначе 409 с пояснением.
    - Если хотя бы один не задан и `policy=fail` — 400 с подсказкой указать `sourceObjectId/targetObjectId`.
  - Создание соединения через `Display.getDefault().syncExec(...)`.
  - Вернуть 201 + DTO нового соединения (в формате, совпадающем с тем, что отдаёт `GET /views/{id}/content`).
- Smoke:
  - Сценарий A: явно указаны оба objectId — 201.
  - Сценарий B: `policy=auto`, однозначное соответствие — 201.
  - Сценарий C: `policy=auto`, неоднозначность — 409.
- Критерий приёмки: соединение появляется в `GET /views/{id}/content` сразу после ответа 201.

Шаг 3. Утилиты/ModelApi
- Цель: инкапсулировать повторное использование логики.
- Действия:
  - В `ModelApi` добавить:
    - `findDiagramObjectById(IDiagramModel view, String objectId)`.
    - `findDiagramObjectsByElementId(IDiagramModel view, String elementId): List<IDiagramModelObject>`.
    - `addRelationToView(IDiagramModel view, IArchimateRelationship rel, IDiagramModelObject s, IDiagramModelObject t)`.
  - Обновить/добавить конвертер `connectionToDto(...)` при необходимости для единообразия ответа.
- Критерий приёмки: HttpHandler становится тоньше, бизнес‑логика — в ModelApi.

Шаг 4. MCP слой (Python)
- Цель: дать агенту инструмент для добавления связи на вид.
- Действия:
  - В `archi-mcp-server/server.py` добавить инструмент:
    ```python
    add_relation_to_view(view_id: str,
                         relation_id: str,
                         source_object_id: Optional[str] = None,
                         target_object_id: Optional[str] = None,
                         policy: str = "auto") -> Dict[str, Any]
    ```
  - Маппинг: `POST /views/{view_id}/add-relation` с телом по OpenAPI.
  - Короткий docstring и `PARAM_DESCRIPTIONS` для параметров.
- Критерий приёмки: инструмент виден в Cursor, выполняется end‑to‑end.

Шаг 5. Документация
- Цель: синхронизировать тексты.
- Действия:
  - `archi-mcp-plugin/README.md`: добавить раздел «Добавление связи на вид», описать `policy` и ошибки.
  - `archi-mcp-plugin/AGENTS.md`: добавить конспект сценария, подсказки по выбору objectId.
  - `archi-mcp-server/README.md` и `AGENTS.md`: описать новый инструмент MCP и пример использования.
- Критерий приёмки: документация короткая и однозначная, без дублирования деталей реализации.

Шаг 6. Smoke‑скрипты и пример
- Цель: воспроизводимость.
- Действия (WSL):
  - Получить объекты вида: `curl -sS "http://127.0.0.1:8765/views/$VID/content" | jq .`
  - Вызвать REST: `curl -sS -X POST "http://127.0.0.1:8765/views/$VID/add-relation" -H 'Content-Type: application/json' -d '{"relationId":"$RID","policy":"auto"}' | jq .`
  - Проверить, что соединение появилось в контенте вида.
- Критерий приёмки: шаги стабильно работают на тестовой модели.

—

Роллбэк‑план
- Откатить HttpHandler и изменения в `ModelApi` (история Git).
- Удалить MCP‑инструмент из `server.py` и тексты из README/AGENTS.

Заметки по рискам
- Вариативность представлений: элемент может встречаться на виде несколько раз — критично корректно обрабатывать неоднозначность.
- Названия типов связей/видов могут отличаться между моделями — не жёстко валидировать строковые типы в MCP, полагаться на 422/404 от REST.
- На ранней альфе избегаем «магических» эвристик; `policy=auto` ограничиваем правилом «ровно одно вхождение на сторону».


