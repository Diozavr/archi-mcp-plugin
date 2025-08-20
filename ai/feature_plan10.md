### План: Script API (feature-plan 10)

## Цель
- Добавить в REST API поддержку выполнения скриптов через установленный Archi Scripting Plugin и минимальный introspection API.
 

## Контекст/Предпосылки
- В `resources/openapi.json` уже заложены маршруты `GET /script/engines` и `POST /script/run`, сейчас помечены как 501 Not Implemented.
- Нужна безопасная и детерминированная интеграция с плагином `com.archimatetool.script.*` (jArchi). Вызовы должны выполняться только локально (`127.0.0.1`).
 
- CI может выполняться без scripting‑плагина, поэтому покрываем оба режима: установлен/не установлен.

## Инварианты
- Локальный сервер, только `127.0.0.1`; порт — как в `Config.resolvePort()`.
- Операции с EMF/SWT — через `Display.getDefault().syncExec(...)`.
- Источник правды REST‑контракта — `resources/openapi.json`.
- Тесты — в `com.archimatetool.mcp/test`.

## Целевая архитектура 

### Слой ядра (core)
- `ScriptingCore`:
  - `boolean isPluginInstalled()` — проверка наличия/активности OSGi‑бандлов `com.archimatetool.script` и под-плагинов (`groovy`, `jruby`, `nashorn`/`premium` и т.п.).
  - `List<String> listEngines()` — перечисление доступных движков (например, `ajs`, `groovy`, `jruby`).
  - `ScriptResult run(ScriptRequest req)` — синхронный запуск скрипта с таймаутом и изоляцией логов.

### HTTP слой
- Новые хендлеры:
  - `ScriptEnginesHttpHandler` → `GET /script/engines`
  - `ScriptRunHttpHandler` → `POST /script/run`
- Маршрутизация — регистрация в `Router`.

### JSON‑контракты
- `/script/engines` ⇒ 200 с `{ installed: boolean, engines: string[] }`.
- `/script/run` ⇒ запрос `{ engine?: string, code: string, timeoutMs?: integer, bindings?: object, modelId?: string, log?: "stdout"|"script" }` и ответ `{ ok: boolean, result?: any, stdout?: string, stderr?: string, durationMs: integer }`. Если плагин недоступен — 501.

## Шаги реализации
1) Контракт
   - Обновить `resources/openapi.json` и `resources/openapi_v91.json`: определить схемы и ответы для `/script/engines` и `/script/run` (200/400/409/501/504).
   - Обновить `AGENTS.md` раздел «Быстрые проверки» и оговорки по синхронизации описаний параметров в `archi-mcp-server/server.py`.
   - Статус: [done]
   - Сделано: обновлены спецификации `openapi.json`/`openapi_v91.json` и примеры в `AGENTS.md`; контракт описывает ответы 200/400/409/501/504.
   - Осталось: —

2) Ядро: `ScriptingCore`
   - Реализовать определение наличия плагина через OSGi (`Platform.getBundle(...)`), собрать список движков по доступным бандлам.
   - Определить DTO `ScriptRequest`, `ScriptResult` и политику таймаута.
   - Статус: [done]
   - Сделано: базовая проверка наличия плагина, перечисление движков `ajs/groovy/jruby`, DTO `ScriptRequest`/`ScriptResult`, `run(...)` через `RunArchiScript` с временным файлом, захват stdout/stderr, таймауты и запуск через UI‑поток.
   - Осталось: —

3) HTTP‑хендлеры
   - Реализовать `ScriptEnginesHttpHandler` (всегда 200; если нет плагина — `{installed:false, engines:[]}`).
   - Реализовать `ScriptRunHttpHandler`:
     - Валидация: обязательное `code` (без `filePath`), опциональный `engine` (дефолт — `ajs`), лимиты `timeoutMs`.
     - При отсутствии плагина — 501.
     - Вызов `ScriptingCore.run(...)`; если движок поддерживает только файлы — создать временный файл с расширением движка, записать `code`, выполнить, затем удалить. Маппинг исключений на 400/409/422/504.
   - Статус: [done]
   - Сделано: добавлены хендлеры, регистрация в `Router`, интеграция с `ScriptingCore.run(...)`, обработка таймаута и расширенная валидация параметров. Юнит‑тесты покрывают сценарии installed/uninstalled, ошибки валидации, маппинг 409 и 504.
   - Осталось: —

4) Логи/безопасность
   - Ограничить максимальные размеры `stdout/stderr` и времени выполнения.
   - Обеспечить локальный доступ (уже есть инвариант биндинга 127.0.0.1).
   - Статус: [done]
   - Сделано: добавлены лимиты на `timeoutMs` и усечение `stdout/stderr`; доступ по‑прежнему ограничен `127.0.0.1`.
   - Осталось: —

5) Документация и сервер LLM
   - В `AGENTS.md` описать примеры вызовов `/script/engines` и `/script/run`.
   - В `archi-mcp-server/server.py` синхронизировать описания параметров (раздел `PARAM_DESCRIPTIONS`).
   - Статус: [done]
   - Сделано: добавлены примеры в `AGENTS.md`; smoke‑скрипт выполняется, требуя запущенный MCP сервер.
   - Осталось: —

## Ведение прогресса по шагам
- Для каждого шага поддерживать чек‑лист:
  - Статус: [done|partial|todo]
  - Сделано: краткий перечень завершённых подпунктов
  - Осталось: оставшиеся подпункты/блокеры/владелец (если есть)

## DoD (Definition of Done)
- `GET /script/engines` возвращает 200 с списком движков или пустым списком при `installed=false`.
- `POST /script/run` выполняет простой скрипт (`ajs`), возвращает `ok=true` и `stdout`/`result`; при отсутствии плагина — 501.
- Обновлён `openapi.json` и примеры в `AGENTS.md`; smoke‑скрипт демонстрирует happy‑path и режим без плагина.
- Юнит‑тесты покрывают валидацию, маппинг ошибок и режимы installed/uninstalled (с заглушкой `ScriptingCore`).

## Тестирование
- Юнит‑тесты: 
  - `ScriptEnginesHttpHandler` — обе ветки installed/uninstalled.
  - `ScriptRunHttpHandler` — валидации (400), no active model (409), таймаут (504, если применимо), uninstalled (501).
- Интеграционные/smoke:
  - Без плагина: `/script/engines` (installed=false), `/script/run` → 501.
  - С плагином: `/script/engines` содержит `ajs`; `/script/run` с простым `print('ok')` возвращает `ok=true` и `stdout` содержит `ok`.

## Риски/роллбэк
- Риски:
  - Наличие разных вариантов scripting‑плагина и движков → неверное определение списка движков. Митигировать за счёт проверки OSGi‑бандлов по известным префиксам и защищённого fallback.
  - Заморозка UI‑потока при долгих скриптах. Митигировать таймаутами и осторожной синхронизацией через `syncExec` только там, где нужно EMF/SWT.
  - Шумные/длинные логи. Митигировать ограничениями на размер и усечением.
- Роллбэк:
  - Вернуть 501 для `/script/*`, откатить `openapi.json` к предыдущей версии.


