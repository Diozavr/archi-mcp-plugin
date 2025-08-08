# AGENTS

Ключевые указания для работы с плагином и его REST API.

## Среда
- ОС: Windows, shell — WSL. Примеры команд — bash.
- Для REST‑проверок используйте `curl` и `jq`.

## Инварианты API
- Биндинг только `127.0.0.1:8765` (локально).
- При отсутствии активной модели — HTTP 409 и `{"error":"no active model"}`.
- Совместимость путей:
  - Новые: `/views/{id}/content`, `/views/{id}/add-element` (POST), подресурсы типа `/views/{id}/objects/...`.
  - Legacy: `/views/content?id=...`, `/views/add-element`.
- `/search` поддерживает повторяющиеся параметры `property=key=value`.

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

## Качество/развитие
- Сохранять legacy‑маршруты до решения о декомиссии.
- Рекомендация: заменить ручной разбор JSON на стандартный парсер без изменения внешнего контракта.
- Не расширять сетевую поверхность (0.0.0.0, аутентификация) без согласования: сервис задуман как локальный.
- При изменениях `resources/openapi.json` (включая тексты `summary/description` и описания параметров)
  необходимо синхронизировать статические описания параметров MCP‑инструментов в `archi-mcp-server/server.py`.
  Для REST‑контракта «истина» — `openapi.json`. `server.py` — только источник описаний для LLM (раздел `PARAM_DESCRIPTIONS`).

## Быстрые проверки
- `GET /status` → `{"ok":true,...}`
- `POST /elements` с типом в kebab-case создаёт элемент.
- `POST /model/save` → `{"saved":true,...}`
