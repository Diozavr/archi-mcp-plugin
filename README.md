# Archi MCP Plugin

Локальный HTTP API поверх активной модели Archi (ArchiMate). Служит бэкендом для MCP‑сервера и может использоваться напрямую через REST (только localhost).

- Хост/порт: http://127.0.0.1:8765
- Запуск: автоматически при старте Archi (Bundle-Activator + org.eclipse.ui.startup)
- Требования: JavaSE 17, Archi (PDE), зависимости `org.eclipse.ui`, `com.archimatetool.editor`

## Настройки
- Порт HTTP сервера можно изменить в Archi → Preferences → MCP.
- Приоритет источников порта: System Property `archi.mcp.port` → Env `ARCHI_MCP_PORT` → Preferences → Default (`8765`).

## Toolbar (MCP Server)
Плагин добавляет отдельную панель инструментов "MCP" с переключателем "MCP Server". Иконка зелёная при запущенном сервере и красная при остановленном. Нажатие запускает или останавливает сервер без перезапуска Archi. Тултип показывает текущий порт (127.0.0.1:PORT). Если панели не видно, включите "MCP" в `Window → Customize Perspective… → Tool Bar Visibility`. При занятом порте отображается сообщение об ошибке.

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

## MCP Server через npx (универсальный способ)
Начиная с версии 2.0, MCP сервер может запускаться через универсальный npx-пакет, который автоматически генерирует MCP инструменты из OpenAPI спецификации:

### Пример `.cursor/mcp.json`
```json
{
  "mcpServers": {
    "archi-api": {
      "command": "npx",
      "args": [
        "-y",
        "@tyk-technologies/api-to-mcp@latest",
        "--spec",
        "http://127.0.0.1:8765/openapi.json"
      ]
    }
  }
}
```

**Преимущества подхода через npx:**
- Автоматическая генерация MCP инструментов из OpenAPI спецификации
- Не требует отдельного MCP сервера
- Всегда актуальные инструменты, синхронизированные с REST API
- Универсальное решение для любых OpenAPI сервисов

## Архитектура
- Вход: `com.archimatetool.mcp.Activator` → `HttpServerRunner` (com.sun.net.httpserver)
- Модель: `ModelApi` (создание/удаление/поиск, DTO, bounds, сохранение)
- UI: изменения модели через `Display.getDefault().syncExec(...)`

## Ограничения
- Слушает только 127.0.0.1
- JSON‑парсинг и сериализация через Jackson
- `/elements` (GET) отсутствует — используйте `/search`
- `/script/*` → 501
