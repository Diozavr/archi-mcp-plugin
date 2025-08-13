# Archi MCP Plugin

Локальный HTTP API поверх активной модели Archi (ArchiMate). Служит бэкендом для MCP‑сервера и может использоваться напрямую через REST (только localhost).

- Хост/порт: http://127.0.0.1:8765
- Запуск: автоматически при старте Archi (Bundle-Activator + org.eclipse.ui.startup)
- Требования: JavaSE 17, Archi (PDE), зависимости `org.eclipse.ui`, `com.archimatetool.editor`

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
- Виды: `GET /views`, `POST /views`, `GET|DELETE /views/{id}`, `GET /views/{id}/content`,
  `GET /views/{id}/image?format=png|svg`, `PATCH /views/{id}/objects/{objectId}/bounds`
- Legacy: `GET /views/content?id=...`, `POST /views/add-element`

## Архитектура
- Вход: `com.archimatetool.mcp.Activator` → `HttpServerRunner` (com.sun.net.httpserver)
- Модель: `ModelApi` (создание/удаление/поиск, DTO, bounds, сохранение)
- UI: изменения модели через `Display.getDefault().syncExec(...)`

## Ограничения
- Слушает только 127.0.0.1
- JSON‑парсинг и сериализация через Jackson
- `/elements` (GET) отсутствует — используйте `/search`
- `/script/*` → 501

## MCP JSON-RPC
- Endpoint: `POST http://127.0.0.1:8765/mcp/`
- List tools: JSON-RPC `{ "jsonrpc":"2.0", "id":1, "method":"tools/list" }`
- Call tool: `{ "jsonrpc":"2.0", "id":1, "method":"tools/call", "params":{"name":"status"} }`
- Example `.cursor/mcp.json`:
```json
{
  "version": 1,
  "services": [{ "url": "http://127.0.0.1:8765/mcp/" }]
}
```
- Smoke test: `com.archimatetool.mcp/test/test_mcp_smoke.sh`
