### План рефакторинга (итерация 2)

Цель: упростить набор инструментов MCP, сделать названия и поведение очевидными для агента, убрать дубли.

—

Инварианты (см. `AGENTS.md`):
- Слушаем только `127.0.0.1:8765`.
- Любые изменения EMF‑модели — через `Display.getDefault().syncExec(...)`.
- Сохраняем формат ответов REST (обратная совместимость по JSON).
- Источник правды для REST — `resources/openapi.json`. `archi-mcp-server/server.py` — источник описаний для LLM.

Процесс выполнения:
- Двигаемся по шагам ниже (1 → …). Каждый шаг — минимальные совместимые изменения, затем smoke‑проверки.
- Коммиты атомарные: `refactor(v2-step-01): ...`.

Smoke‑проверки (WSL):
```bash
curl -sS http://127.0.0.1:8765/status | jq .
curl -sS "http://127.0.0.1:8765/search?q=review&includeDocs=true&debug=true" | jq .
```

Критерии готовности итерации:
- В MCP доступен один инструмент поиска — `search` (богатый по параметрам).
- Инструмент `element_relations` отсутствует в MCP.
- Документация и описания параметров в MCP актуальны и короткие.

—

Шаг 1. MCP API: сделать `search` основным и единым
- Цель: убрать раздвоение поисковых инструментов.
- Действия:
  - В `archi-mcp-server/server.py` переименовать `search_advanced` → `search` (сохранить сигнатуру и описания параметров). Текущий облегчённый `search` удалить.
  - Обновить `PARAM_DESCRIPTIONS` под новое имя.
  - Короткий docstring у `search`: "Advanced search with filters" + одна строка про OR‑семантику `q` и AND‑фильтры `property`.
- Smoke:
  - В Cursor виден один инструмент `search` с параметрами `q/kind/element_type/relation_type/include_docs/include_props/model_id/limit/offset/properties/debug/log_target`.
  - Запросы к `/search` работают как прежде.
- Критерий приёмки: поиск выполняется и даёт те же результаты, подсказки отображаются.

Шаг 2. MCP API: удалить `element_relations`
- Цель: исключить дублирование с `get_element(..., include_relations, include_elements)`.
- Действия:
  - В `archi-mcp-server/server.py` удалить инструмент `element_relations` и все упоминания в описаниях.
  - В `PARAM_DESCRIPTIONS` удалить блок для `element_relations`.
  - В README/AGENTS для сервера заменить примеры на использование `get_element` с `include_relations=true` и `include_elements=true`.
- Примечание: REST‑эндпоинт `/elements/{id}/relations` можно оставить (для отладки/будущих нужд) или удалить позже отдельным шагом; JSON‑форматы не меняем.
- Smoke:
  - В Cursor инструмента `element_relations` нет.
  - `get_element(..., include_relations=true)` возвращает связи; при `include_elements=true` — инлайн DTO конечных точек.
- Критерий приёмки: сценарии с связями полностью закрываются через `get_element`.

Шаг 3. Документация и конфиги
- Цель: синхронизировать тексты.
- Действия:
  - Обновить `archi-mcp-server/README.md` и `AGENTS.md` (разделы про поиск и связи).
  - В `archi-mcp-plugin/README.md` обновить описание поиска (OR по `name/doc/props` при флагах) и получение связей через `GET /elements/{id}?include=relations&includeElements=true`.
  - В `archi-mcp-plugin/AGENTS.md` оставить правило синхронизации описаний: правки в `openapi.json` → обновить `server.py`/`PARAM_DESCRIPTIONS`.
- Критерий приёмки: в документации нет упоминаний `search_advanced` и `element_relations` на стороне MCP.

Шаг 4. Опционально: зачистка OpenAPI (после стабилизации)
- Цель: избегать дребезга в спецификации на ранней альфе.
- Вариант A (минимальный): ничего не менять, оставить `/elements/{id}/relations` в спецификации.
- Вариант B (жёсткий): удалить `/elements/{id}/relations` из `openapi.json` и соответствующий хендлер, если нигде не используется.
- Критерий приёмки: MCP‑клиент полностью покрывается оставшимися маршрутами; smoke‑проверки зелёные.

—

Роллбэк‑план
- При откате шагов: вернуть `search_advanced` под прежним именем и восстановить инструмент `element_relations` из истории Git.

Заметки по рискам
- Переименование `search_advanced` → `search` может потребовать обновления `mcp.json`/клиентов, если они ссылаются по имени инструмента.
- Удаление `element_relations` безопасно для MCP‑клиентов, использующих `get_element` (рекомендуемый путь).



