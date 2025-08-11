### План внедрения: расширение тестов REST (feature-plan 4.1)

Цели:
- Расширить smoke-тест `archi-mcp-server/test_rest_flow.sh` для проверки всех доступных REST‑эндпоинтов (включая legacy‑маршруты) по `resources/openapi.json`.
- Расширить покрытие unit‑тестами внутри плагина (`com.archimatetool.mcp/test`) [[memory:5587551]].

—

Инварианты (см. `AGENTS.md`):
- Локальный сервер: `http://127.0.0.1:8765`.
- При отсутствии активной модели — HTTP 409 с `{ "error": "no active model" }`.
- Совместимость путей: поддерживаем и новые (`/views/{id}/...`), и legacy (`/views/content?id=...`, `/views/add-element`).
- Источник правды по контракту — `resources/openapi.json`; smoke проверяет фактическое соответствие.
- Среда запуска smoke: Windows + WSL, утилиты `curl`, `jq` (скрипт сам подбирает путь к curl).

—

Шаги: расширение smoke‑теста `test_rest_flow.sh`

1) Общие проверки/метаданные
- GET `/status` — базовая проверка ok:true (уже есть).
- GET `/openapi.json` — спецификация доступна; валидация на непустой JSON.
- GET `/types` — списки `elementTypes`, `relationTypes`, `viewTypes` непустые.

2) Структура модели (папки)
- GET `/folders` — дерево папок возвращается; проверить, что корни есть.
- POST `/folder/ensure` — создать/обеспечить путь, например `"Diagrams/Smoke"`; убедиться, что вернулся валидный узел.

3) Виды (views)
- GET `/views` — список доступных видов; сохранить произвольный `VID0` (как сейчас) для совместимости.
- POST `/views` — создать новый `ArchimateDiagramModel` с именем `"Smoke View"`; сохранить `VID1`.
- GET `/views/{VID1}` — получить DTO вида.
- GET `/views/{VID1}/content` — получить пустое содержимое.
- GET `/views/content?id={VID1}` — legacy‑вариант (должен совпадать по смыслу).

4) Элементы (elements)
- POST `/elements` — создать 2 элемента разных типов (как сейчас), сохранить `E1`, `E2`.
- GET `/elements/{E1}` — получить DTO.
- GET `/elements/{E1}?include=relations&includeElements=true` — убедиться, что поле `relations` присутствует и корректно по структуре.
- GET `/elements/{E1}/relations?direction=out` — проверить фильтр направления, затем `in`, затем `both&includeElements=true`.
- PATCH `/elements/{E1}` — обновить `name`, проверить изменение.

5) Отношения (relations)
- POST `/relations` — создать отношение между `E1` → `E2`; сохранить `REL`.
- GET `/relations/{REL}` — получить DTO отношения.
- PATCH `/relations/{REL}` — переименовать; проверить изменение.

6) Операции на виде (объекты и связи)
- POST legacy `/views/add-element` — добавить `E1` на `VID1` (как сейчас); сохранить `OBJ1`.
- POST `/views/{VID1}/add-element` — добавить `E2` в контейнер `OBJ1` с `bounds`; сохранить `OBJ2`.
- PATCH `/views/{VID1}/objects/{OBJ2}/bounds` — изменить габариты, проверить, что вернулся обновлённый DTO.
- PATCH `/views/{VID1}/objects/{OBJ2}/move` — переместить `OBJ2` в корень (`parentObjectId=0`) и обратно в `OBJ1` (две операции); проверить `parentObjectId` в DTO; один прогон с `keepExistingConnection=false` (поведение по умолчанию), второй — с `true`.
- POST `/views/{VID1}/add-relation` — добавить отношение `REL` на вид:
  - Вариант A: без `sourceObjectId/targetObjectId` с `policy=auto` — должен сработать при единственных вхождениях.
  - Вариант B: указать `sourceObjectId=OBJ1`, `targetObjectId=OBJ2` и `suppressWhenNested=true` — ожидаем `{ suppressed: true }`.
- GET `/views/{VID1}/content` — убедиться, что объекты/связи появились.
- DELETE `/views/{VID1}/objects/{OBJ2}` — удалить вложенный объект; код 204.

7) Рендер изображения вида
- GET `/views/{VID1}/image?format=png&scale=1.0&bg=transparent&margin=0` — сохранить в файл, проверить `Content-Type: image/png` и непустой размер.
  - Негативный случай: `format=svg` — до включения SVG вернуть 400 (синхронизация с планом feature-plan 4 / шаг 7).

8) Поиск
- GET `/search?q=Actor` — базовая проверка (как сейчас).
- GET `/search?q=Actor&kind=element&limit=1&offset=0&debug=true` — проверить наличие блока `debug`.

9) Сохранение модели
- POST `/model/save` — как сейчас, `{ saved: true }`.

10) Скриптовые заглушки (Not Implemented)
- GET `/script/engines` — код 501.
- POST `/script/run` — код 501.

11) Ошибки/валидации (точечные проверки)
- 404: GET `/elements/{random}` и `/views/{random}` — not found.
- 405: Неверные методы на известных путях (например, POST на `/status`).
- 409: Любой путь, требующий активную модель, в окружении без модели (опционально отдельным прогоном).

12) Уборка
- DELETE `/relations/{REL}` — 204.
- DELETE `/elements/{E1}`, `/elements/{E2}` — 204.
- DELETE `/views/{VID1}` — 204.

Подсказка по реализации: добавлять шаги в существующий `test_rest_flow.sh`, сохраняя текущую структуру/WSL‑совместимость и детализированные `echo`‑метки. Для бинарных ответов (image) использовать `-o` и проверять тип/размер.

—

Шаги: расширение unit‑тестов (`com.archimatetool.mcp/test`)

1) Инфраструктура тестов
- Рекомендация: завести утилиту `FakeHttpExchange` для тестирования хендлеров без реального HTTP‑сервера (in‑memory запрос/ответ).
- Для тестов, затрагивающих EMF/SWT, использовать `Display.getDefault().syncExec(...)` внутри тестовых хелперов (как в коде плагина).
- Для интеграционных кейсов открыть тестовую модель из `testdata/Archisurance.archimate` через `ActiveModelService` и закрывать её после теста.

2) JSON/утилиты
- JsonReaderTest — расширить кейсы: `optIntWithin`, обрабатывание неверных типов/пустого тела/битого JSON; boolean‑парсинг; значения по умолчанию.
- JsonUtilTest — Unicode, экранирование управляющих символов, сериализация вложенных структур.
- StringCaseUtilTest — уже есть; дополнить кейсы kebab→camel и обратную совместимость с нестандартными типами.

3) Хендлеры: позитивные сценарии
- TypesHttpHandlerTest — списки типов непустые; содержат базовые элементы/отношения.
- FoldersHttpHandlerTest — дерево содержит корневые папки; глубина > 0 на тестовой модели.
- FolderEnsureHttpHandlerTest — создаёт вложенный путь и возвращает валидный узел.
- ViewsHttpHandlerTest — создание `ArchimateDiagramModel`, получение, список.
- ViewItemHttpHandlerTest —
  - `GET /views/{id}` → корректный DTO;
  - `GET /views/{id}/content` → корректные поля;
  - `POST /views/{id}/add-element` → возврат `objectId`;
  - `PATCH /views/{id}/objects/{objectId}/bounds` → обновлённые габариты;
  - `PATCH /views/{id}/objects/{objectId}/move` → смена контейнера; проверка `keepExistingConnection`;
  - `POST /views/{id}/add-relation` → ветки 201 и 200 (suppressed).
- ElementsHttpHandlerTest — создание элемента, 400 при отсутствии `type|name`.
- ElementItemHttpHandlerTest —
  - `GET /elements/{id}` с `include=relations`, `includeElements=true`;
  - `GET /elements/{id}/relations?direction=in|out|both`.
- RelationsHttpHandlerTest — создание отношения; 404/400 при некорректных ссылках.
- RelationItemHttpHandlerTest — получение, переименование, удаление.
- ModelSaveHttpHandlerTest — `{ saved: true }`.

4) Хендлеры: негативные сценарии/валидации
- Общие 405 для неверных методов.
- View image: `format=svg` → 400 (до внедрения SVG по feature‑plan 4 шаг 7).
- Add‑relation: 422 при несоответствии `sourceObjectId/targetObjectId` концам отношения; 409 при неоднозначном `policy=auto`.
- Move: 400 при попытке переместить объект внутрь собственного потомка (цикл).
- FolderEnsure: 400 при пустом `path`.

5) Модельный слой/сервисы (точечные тесты)
- ModelApiTest — `viewToDto/viewContentToDto/viewObjectToDto/connectionToDto`, `findDiagramObjectById`, `findDiagramObjectsByElementId`, `isAncestorOf`.
- ElementServiceTest/RelationServiceTest/ViewServiceTest — happy‑path операции, удаление, перемещения.

—

Критерии готовности (DoD)
- Smoke‑скрипт покрывает все маршруты из `openapi.json` и ключевые legacy‑пути; позитивные и несколько негативных кейсов; успешно проходит локально в WSL.
- Unit‑тесты покрывают основные хендлеры, утилиты и сервисы, включая негативные ветки; тесты зелёные при headless запуске.
- Документация: в `AGENTS.md` кратко перечислены новые smoke‑шаги и как открыть тестовую модель перед запуском.


