# AGENTS

Ключевые указания для работы с плагином и его REST API.

## Среда
- ОС: Windows, shell — WSL. Примеры команд — bash.
- Для REST‑проверок используйте `curl` и `jq`.

## Конфигурация
- Порт HTTP сервера MCP задаётся через System Property `archi.mcp.port`, переменную окружения `ARCHI_MCP_PORT` или Preferences (Archi → Preferences → MCP). Приоритет: System Property → Env → Preferences → Default (`8765`).

## Инварианты API
- Биндинг только `127.0.0.1:8765` (локально).
- При отсутствии активной модели — HTTP 409 и `{"error":"no active model"}`.
- Совместимость путей:
  - Новые: `/views/{id}/content`, `/views/{id}/add-element` (POST), подресурсы типа `/views/{id}/objects/...`.
  - Legacy: `/views/content?id=...`, `/views/add-element`.
- `/search` поддерживает повторяющиеся параметры `property=key=value`.
- `/views/{id}/image` поддерживает `format=png|svg`; `dpi` применимо только к PNG.`

### Семантика поиска
- `q` ищется по OR:
  - всегда по `name`
  - по `documentation`, если `includeDocs=true`
  - по `properties` (key/value), если `includeProps=true`
- `property=key=value` — строгие AND‑фильтры поверх текстового поиска.
- `kind` сужает область до `element|relation|view`.
- `debug=true` добавляет в ответ поле `debug` со счётчиками и примерами.

## Работа с моделью
- Любые изменения EMF‑модели — через `org.eclipse.swt.widgets.Display.getDefault().syncExec(...)`.
- Поиск объектов по id — `ArchimateModelUtils.getObjectByID(...)`.

## Архитектура
- Бизнес‑логика вынесена в пакет `com.archimatetool.mcp.core.*`.
- REST‑хендлеры и JSON‑RPC контроллер лишь парсят протокол и делегируют вызовы в ядро.
- Ядро выбрасывает `CoreException`‑подтипы; `ResponseUtil` и MCP маппят их на HTTP/JSON‑RPC ошибки.

## Качество/развитие
- Сохранять legacy‑маршруты до решения о декомиссии.
- JSON обрабатывается через библиотеку Jackson.
- Не расширять сетевую поверхность (0.0.0.0, аутентификация) без согласования: сервис задуман как локальный.
- При изменениях `resources/openapi.json` (включая тексты `summary/description` и описания параметров)
  необходимо синхронизировать статические описания параметров MCP‑инструментов в `archi-mcp-server/server.py`.
  Для REST‑контракта «истина» — `openapi.json`. `server.py` — только источник описаний для LLM (раздел `PARAM_DESCRIPTIONS`).

## Быстрые проверки
- `GET /status` → `{"ok":true,...}`
- `POST /elements` с типом в kebab-case создаёт элемент.
- `POST /model/save` → `{"saved":true,...}`
- Расширенный smoke‑тест: `com.archimatetool.mcp/test/test_smoke.sh` покрывает маршруты
  `/openapi.json`, `/types`, `/folders`, создание видов/элементов/отношений,
  операции над объектами вида, `/views/{id}/image`, `/search`, сценарии ошибок и уборку.
  Перед запуском откройте тестовую модель `testdata/Archisurance.archimate` в Archi или
  обеспечьте активную модель.

## MCP JSON-RPC
- Эндпоинт: `POST /mcp` (JSON-RPC 2.0, только localhost).
- Методы протокола: `initialize`, `notifications/initialized`, `tools/list`, `tools/call`.
- Запрос: `{ "jsonrpc":"2.0", "id":1, "method":"tools/list", "params":{} }`.
- Вызов инструмента: `{ "jsonrpc":"2.0", "id":2, "method":"tools/call", "params":{"name":"status","args":{}} }`.
- Успех: `{ "jsonrpc":"2.0", "id":1, "result":{...} }`.
- Ошибка: `{ "jsonrpc":"2.0", "id":1, "error":{"code":-32004,"message":"..."} }`.
- Уведомления (без `id`) возвращают HTTP 204 без тела.
- Ошибки ядра маппятся на диапазон `-32000..-32099`:
  - BadRequest → `-32001`
  - NotFound → `-32004`
  - Conflict → `-32009`
  - Unprocessable → `-32022`
- Бинарные данные (например, изображения вида) отдаются как `{ "data_base64": "...", "content_type": "image/png" }`.
- `tools/list` перечисляет все доступные методы с описаниями и параметрами.
