# Archi MCP Plugin

Локальный HTTP API поверх активной модели Archi (ArchiMate). Служит бэкендом для MCP‑сервера и может использоваться напрямую через REST (только localhost).

- Хост/порт: http://127.0.0.1:8765
- Запуск: автоматически при старте Archi (Bundle-Activator + org.eclipse.ui.startup)
- Требования: JavaSE 11, Archi (PDE), зависимости `org.eclipse.ui`, `com.archimatetool.editor`

## Быстрый старт (PDE/Eclipse)
1) Импортируйте `com.archimatetool.mcp` как PDE‑плагин.
2) Запустите Eclipse Application (Archi). Плагин стартует автоматически.
3) Откройте модель в Archi.
4) Проверка:
```bash
curl -s http://127.0.0.1:8765/status
```

## Эндпоинты (MVP)
- Сервис: `GET /status`, `GET /openapi.json`, `GET /types`
- Папки/поиск: `GET /folders`, `POST /folder/ensure`, `GET /search`
- Элементы: `POST /elements`, `GET|PATCH|DELETE /elements/{id}`
  - Обогащение ответа элемента через query: `?include=relations[&includeElements=true]`
- Связи: `POST /relations`, `GET|PATCH|DELETE /relations/{id}`
- Виды: `GET /views`, `POST /views`, `GET|DELETE /views/{id}`, `GET /views/{id}/content`, `PATCH /views/{id}/objects/{objectId}/bounds`
- Legacy: `GET /views/content?id=...`, `POST /views/add-element`

## Архитектура
- Вход: `com.archimatetool.mcp.Activator` → `HttpServerRunner` (com.sun.net.httpserver)
- Модель: `ModelApi` (создание/удаление/поиск, DTO, bounds, сохранение)
- UI: изменения модели через `Display.getDefault().syncExec(...)`

## Ограничения
- Слушает только 127.0.0.1
- JSON‑парсинг упрощён (регекспы)
- `/elements` (GET) отсутствует — используйте `/search`
- `/script/*` → 501
