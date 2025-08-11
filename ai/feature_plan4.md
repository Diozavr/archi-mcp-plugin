### План внедрения: Переход на Archi 5.x, полное обновление JSON-стека + SVG-изображения (feature-plan 4)

Цели:
- Перейти на использование Archi 5.x, Java 17.
- Полностью заменить JSON-стек: отказаться от `minimal-json` и самописного сериализатора; перейти на Jackson (чтение/запись), унифицировать обработку JSON и кодировки.
- Добавить полноценную поддержку SVG для экспорта вида вместе с уже реализованным PNG.
- Сохранить внешний REST-контракт и совместимость legacy-маршрутов.

—

Инварианты (см. `AGENTS.md`):
- Слушаем только `127.0.0.1:8765`.
- Операции с SWT/EMF — через `Display.getDefault().syncExec(...)`.
- Источник правды по REST — `resources/openapi.json`; тексты подсказок MCP в `archi-mcp-server/server.py` синхронизируются.
- Тесты размещаем под `com.archimatetool.mcp/test` внутри плагина [[memory:5587551]].

Процесс:
- Выполняем шагами 1 → N, минимальные атомарные изменения, с короткими smoke‑проверками после каждого шага.
- Отметка выполнения каждого шага в этом файле в соотвесттвующем пункте.

—

Шаг 1. Базовая совместимость Archi 5.x / Java 17 — [x]
- Манифест `META-INF/MANIFEST.MF`:
  - `Bundle-RequiredExecutionEnvironment: JavaSE-17` (было JavaSE-11).
  - Для `Require-Bundle` добавить диапазоны: `com.archimatetool.editor;bundle-version="[5.0.0,6.0.0)", com.archimatetool.model;bundle-version="[5.0.0,6.0.0)"`.
- Пересобрать против таргет-платформы Archi 5.x (PDE Target). Убедиться, что импорты `com.archimatetool.*` резолвятся.

Шаг 2. Подключение Jackson — [x]
- Добавить библиотеки в `lib/` плагина: `jackson-core`, `jackson-databind`, `jackson-annotations` (LTS-совместимые версии для Java 17).
- Обновить `build.properties` → `bin.includes` добавить банки Jackson; `Bundle-ClassPath` в манифесте — тоже.
- Временно сохранить `minimal-json` параллельно, пока не завершена миграция (будет удалён на шаге очистки).

Шаг 3. Единый JSON‑утилитарий — [x]
- В `com.archimatetool.mcp.server` создать `JacksonJson.java` (или в `http` слое), предоставляющий:
  - `static ObjectMapper mapper()` — один процессный синглтон с нужной настройкой (`WRITE_DATES_AS_TIMESTAMPS=false`, pretty = off, UTF-8).
  - `static byte[] writeBytes(Object value)` → сериализация в UTF‑8.
  - `static <T> T read(InputStream)` / `readTree(InputStream)`.
- Заменить `JsonUtil.writeJson(...)` на реализацию через Jackson. Сохранить прежние сигнатуры `ResponseUtil`.

Шаг 4. Миграция чтения JSON запроса — [x]
- Переписать `JsonReader` на Jackson (`JsonNode`) с сохранением удобных методов:
  - `optString`, `optInt`, `optBool`, `optObject`, `optIntWithin`.
  - Реализации должны быть null‑safe, без исключений на некорректном типе.
- Обновить все HTTP‑хендлеры, использующие старый `JsonReader`, на новый (импорты остаются в том же пакете, чтобы диффы были минимальными).

Шаг 5. Миграция записи JSON ответа — [x]
- Удалить ручной сериализатор строк в `JsonUtil.toJson(...)`. Все ответы формируются `ObjectMapper` из `Map/List/POJO`.
- Проверить экранирование и Unicode (контрольные символы <0x20, U+2028/U+2029) — Jackson справляется корректно.

Шаг 6. Рефакторинг парсинга query‑параметров — [x]
- Ввести утилиту `QueryParams` (например, `com.archimatetool.mcp.http.QueryParams`) с поддержкой:
  - Декодирование percent‑encoding в UTF‑8;
  - Повторяющиеся ключи (`property=key=value` в `/search` → список);
  - Дефолты и валидация типов (int/float/bool) с безопасными дефолтами.
- Заменить ручной разбор в `ViewItemHttpHandler` (ветка `image`) и других местах на `QueryParams`.

Шаг 7. SVG‑рендеринг — [x]
- В `ModelApi` добавить `byte[] renderViewToSVG(IDiagramModel view, float scale, String bg, int margin)`:
  - Использовать доступный в Archi 5.x экспортёр SVG (например, `DiagramModelSVGExporter` или эквивалент) в UI‑треде.
  - Вернуть UTF‑8 байты SVG. Учесть `scale`/`margin`/`bg` (если поддерживается, иначе документировать ограничение).
- В `ViewItemHttpHandler` в ветке `image`:
  - Поддержать `format=svg` → `Content-Type: image/svg+xml` и UTF‑8 тело;
  - Для PNG оставить текущую реализацию; `dpi` применять только к PNG.

Шаг 8. OpenAPI и сервер MCP — [x]
- `resources/openapi.json` уже содержит `image/svg+xml` в ответах — синхронизировать тексты `summary/description` при необходимости.
- В `archi-mcp-server/server.py` (инструмент MCP):
  - Обновить функцию получения изображения вида так, чтобы корректно обрабатывала `format=svg` и возвращала правильный `content_type`.
  - Обновить `PARAM_DESCRIPTIONS` в соответствии с OpenAPI.

Шаг 9. Тесты и smoke‑проверки — [x]
- Юнит‑тесты JSON:
  - `test/JsonReaderTest.java`: кейсы пустого/битого JSON, типовые преобразования, вложенные поля.
  - `test/JsonUtilTest.java`: сериализация Map/List/Unicode/escape.
- Юнит‑тесты изображений (минимум):
  - Рендер mock‑вида в PNG/SVG (проверка непустых байтов, корректных заголовков MIME через хендлер).
- Разместить тесты под `com.archimatetool.mcp/test` [[memory:5587551]].
- Smoke (WSL):
  ```bash
  VID="<view-id>"
  # PNG
  curl -sS -o /tmp/v.png "http://127.0.0.1:8765/views/$VID/image?format=png&scale=1.25&bg=transparent&margin=8" && file /tmp/v.png && ls -l /tmp/v.png
  # SVG
  curl -sS -o /tmp/v.svg "http://127.0.0.1:8765/views/$VID/image?format=svg&scale=1&bg=%23ffffff&margin=4" && head -n 2 /tmp/v.svg
  # JSON create element
  curl -sS -X POST http://127.0.0.1:8765/elements -H 'Content-Type: application/json' \
       -d '{"type":"BusinessActor","name":"Agent JSON"}' | jq .
  ```

Шаг 10. Очистка и лицензии — [x]
- Удалить `minimal-json-0.9.5.jar` из `lib/`, ссылки из `bin.includes` и `Bundle-ClassPath`.
- Удалить старую реализацию сериализации из `JsonUtil` (либо целиком удалить файл, если весь функционал перенесён в Jackson‑утилиту).
- Обновить `README.md` и `AGENTS.md` (разделы про SVG/параметры и упоминание перехода на Jackson).
- Проверить наличие и корректность лицензий для добавленных библиотек (Jackson: Apache 2.0). Добавить в `LICENSE.txt`/about.

Шаг 11. Риски/ограничения/роллбэк — [x]
- Риски:
  - Возможные различия в форматировании JSON по сравнению с прежним самописным сериализатором — контракт ответа остаётся прежним (структуры), порядок полей не гарантируется.
  - На некоторых платформах SWT установка DPI для PNG может игнорироваться — считаем некритичным.
- Роллбэк:
  - Вернуть `minimal-json` и прежний `JsonUtil`, отключить Jackson в `Bundle-ClassPath` и `bin.includes`.
  - Отключить ветку `format=svg` в хендлере `/views/{id}/image`.

Критерии готовности (DoD)
- Плагин собирается под Java 17, запускается в Archi 5.x, `GET /status` отдаёт корректный JSON.
- Все пути, ранее возвращавшие JSON, формируют ответы через Jackson; юнит‑тесты JSON зелёные.
- `GET /views/{id}/image?format=png|svg` работает; корректные `Content-Type`; smoke‑проверки проходят.
- Тесты размещены в `com.archimatetool.mcp/test` и выполняются.


