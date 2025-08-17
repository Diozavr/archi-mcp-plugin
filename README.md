# Archi MCP Plugin

Локальный HTTP API поверх активной модели Archi (ArchiMate). Служит бэкендом для MCP‑сервера и может использоваться напрямую через REST (только localhost).

- Хост/порт: http://127.0.0.1:8765
- Запуск: автоматически при старте Archi (Bundle-Activator + org.eclipse.ui.startup)
- Требования: JavaSE 17, Archi (PDE), зависимости `org.eclipse.ui`, `com.archimatetool.editor`, `com.archimatetool.model`

## Настройки
- Порт HTTP сервера можно изменить в Archi → Preferences → MCP.
- Приоритет источников порта: System Property `archi.mcp.port` → Env `ARCHI_MCP_PORT` → Preferences → Default (`8765`).

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

## MCP JSON-RPC
- Эндпоинт: `POST /mcp` (JSON-RPC 2.0).
- Поддерживаются методы `initialize`, `notifications/initialized`, `tools/list`, `tools/call`.
- Пример вызова списка инструментов:
  ```bash
  curl -s -X POST http://127.0.0.1:8765/mcp \
    -H 'Content-Type: application/json' \
    -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | jq .
  ```
- Пример вызова инструмента:
  ```bash
  curl -s -X POST http://127.0.0.1:8765/mcp \
    -H 'Content-Type: application/json' \
    -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"status","args":{}}}' | jq .
  ```
- Ошибки маппятся на коды JSON-RPC (`-32001` BadRequest, `-32004` NotFound и т.д.).
- Уведомления (запросы без `id`) возвращают `HTTP 204` без тела.
- Бинарные данные (например, изображение вида) выдаются в полях `data_base64` + `content_type`.

### Пример `.cursor/mcp.json`
```json
{
  "url": "http://127.0.0.1:8765/mcp",
  "caps": []
}
```

## Сборка и тесты Maven
В каталоге `com.archimatetool.mcp`:

Перед запуском убедитесь, что в Codex заданы переменные `http_proxy`/`https_proxy` для доступа Maven к интернету.
В каталоге `com.archimatetool.mcp` прокси также прописан в `.mvn/settings.xml` (http://proxy:8080);
при сборке вне Codex скорректируйте или удалите этот файл.
Бинарные зависимости (Archi/Eclipse/Jackson) не хранятся в репозитории — скрипт `com.archimatetool.mcp/fetch-archi-deps.sh` скачивает библиотеки Archi, остальные подтягивает Maven.

```bash
cd com.archimatetool.mcp
./fetch-archi-deps.sh  # загрузка библиотек Archi (один раз)
mvn package            # сборка JAR
mvn test               # запуск unit-тестов
```

## Архитектура
- Вход: `com.archimatetool.mcp.Activator` → `HttpServerRunner` (com.sun.net.httpserver)
- Модель: `ModelApi` (создание/удаление/поиск, DTO, bounds, сохранение)
- UI: изменения модели через `Display.getDefault().syncExec(...)`

## Ограничения
- Слушает только 127.0.0.1
- JSON‑парсинг и сериализация через Jackson
- `/elements` (GET) отсутствует — используйте `/search`
- `/script/*` → 501
