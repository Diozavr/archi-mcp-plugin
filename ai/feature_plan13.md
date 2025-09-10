### План: MCP Toolbar в Archi (feature-plan 13)

## Цель
- Сделать отдельную панель инструментов "MCP" в Archi с переключателем "MCP Server" (start/stop), корректной индикацией состояния (зелёная/красная иконка), актуальным тултипом с портом `127.0.0.1:PORT` и видимостью через `Window → Customize Perspective… → Tool Bar Visibility`.

## Контекст/Предпосылки
- Среда: Archi 5.x, плагин `com.archimatetool.mcp` (Java 17, Eclipse RCP).
- Текущий `plugin.xml` добавляет команду в `org.eclipse.ui.menus` с `locationURI="toolbar:com.archimatetool.mcp.toolbar"`, но контейнер тулбара не объявлен. Из-за этого кнопка не отображается.
- Уже реализованы команда `com.archimatetool.mcp.commands.toggleServer` и `ToggleServerHandler` (реализует `IElementUpdater`), автозапуск сервера (`Bundle-Activator + org.eclipse.ui.startup`).
- Требуется создать отдельный тулбар MCP как контейнер на главной панели инструментов `org.eclipse.ui.main.toolbar`.

## Инварианты
- Локальный сервер, только `127.0.0.1`; порт — как в `Config.resolvePort()`.
- Операции с EMF/SWT — через `Display.getDefault().syncExec(...)`.
- Источник правды REST‑контракта — `resources/openapi.json`.
- Тесты — в `com.archimatetool.mcp/test`.

## Целевая архитектура 

### Аспект архитектуры 1: UI-вклад (меню/тулбары)
- В `org.eclipse.ui.menus` объявить новый контейнер-тулбар `com.archimatetool.mcp.toolbar` в составе главной панели инструментов `org.eclipse.ui.main.toolbar` и положить в него toggle‑команду.
- Рекомендуемое размещение: `?after=additions`.
- Пример целевого фрагмента `plugin.xml` (пояснение, не код-источник):
```xml
<extension point="org.eclipse.ui.menus">
  <menuContribution locationURI="toolbar:org.eclipse.ui.main.toolbar?after=additions">
    <toolbar id="com.archimatetool.mcp.toolbar" label="MCP">
      <command
        commandId="com.archimatetool.mcp.commands.toggleServer"
        label="MCP Server"
        style="toggle"
        icon="img/mcp_off.png"
        tooltip="Start/Stop MCP Server"/>
    </toolbar>
  </menuContribution>
</extension>
```

### Аспект архитектуры 2: Команда/обработчик
- `ToggleServerHandler` оставляем; он обновляет `checked`, tooltip, иконки и текст через `IElementUpdater` + `ICommandService.refreshElements(...)`.
- Состояние (running/Stopped) берём из `Activator`.

### Аспект архитектуры 3: UX и интеграция
- Панель видна по умолчанию в чистом воркспейсе; при отсутствии — доступна через `Window → Customize Perspective… → Tool Bar Visibility → MCP`.
- При занятом порте — показываем `MessageDialog` с ошибкой (уже реализовано в обработчике).

## Ведение прогресса по шагам (скопировать в план целиком)
- Для каждого шага поддерживать чек‑лист:
  - Статус: [done|partial|todo]
  - Сделано: краткий перечень завершённых подпунктов
  - Осталось: оставшиеся подпункты/блокеры/владелец (если есть)
- Если шаг выполнен, агент сразу переходит к следующему до полного завершения плана.

## Шаги реализации
⚠️ Важно: все шаги должны выполняться последовательно и без остановок, пока не завершится весь план.
1) Объявить отдельный тулбар MCP в `plugin.xml`
   - Заменить текущий `menuContribution` с `locationURI="toolbar:com.archimatetool.mcp.toolbar"` на контейнер `toolbar:org.eclipse.ui.main.toolbar?after=additions`.
   - Внутри объявить `<toolbar id="com.archimatetool.mcp.toolbar" label="MCP">` и поместить команду `toggleServer` со стилем `toggle`, `icon="img/mcp_off.png"`, `label="MCP Server"`, `tooltip`.
   - Проверить, что других конфликтующих вкладов меню/тулбара не осталось.
   - Статус: [todo]
   - Сделано: —
   - Осталось: правка `plugin.xml`, сборка, локальный запуск

2) Проверить иконки/ресурсы
   - Убедиться, что `img/mcp_on.png` и `img/mcp_off.png` доступны по путям и корректного размера (16×16). `@2x` оставить как есть (Eclipse возьмёт обычные 16×16 для тулбара).
   - Статус: [todo]
   - Сделано: —
   - Осталось: визуальная проверка в рантайме

3) Верифицировать обновление состояния
   - Убедиться, что `ToggleServerHandler#updateElement(...)` меняет `checked`, `icon`, `tooltip`, и что `Activator.refreshToggleState()` триггерит `ICommandService.refreshElements(...)`.
   - Статус: [todo]
   - Сделано: —
   - Осталось: ручная проверка при старте/остановке

4) PDE-проверка в Archi
   - Запустить Eclipse Application (Archi) из PDE.
   - Проверить видимость панели MCP. Если не видна — включить в `Window → Customize Perspective… → Tool Bar Visibility`.
   - Нажатие по кнопке должно запускать/останавливать сервер, иконки меняются, тултип показывает актуальный порт `127.0.0.1:PORT`.
   - Статус: [todo]
   - Сделано: —
   - Осталось: пройти сценарии

5) Документация
   - Актуализировать раздел `README.md → Toolbar (MCP Server)` при необходимости (пояснить про Customize Perspective, если нужно).
   - Статус: [todo]
   - Сделано: —
   - Осталось: ревью текста

6) Мини‑smoke проверки REST
   - С открытой моделью в Archi выполнить:
     - `curl -s http://127.0.0.1:8765/status | jq .ok` → `true`
     - Остановить сервер кнопкой → `curl` должен упасть по соединению/вернуть ошибку.
     - Запустить сервер снова → `curl` снова возвращает `ok:true`.
   - Статус: [todo]
   - Сделано: —
   - Осталось: выполнить команды

## DoD (Definition of Done)
- В чистом воркспейсе Archi панель "MCP" видна на главной панели инструментов (или доступна через Customize Perspective в существующем воркспейсе).
- Кнопка "MCP Server" переключает состояние сервера без перезапуска Archi; иконки и тултип корректны, отражают порт.
- Ошибки (занятый порт и т.п.) показываются диалогом.
- README содержит актуальные инструкции.
- Юнит‑ и smoke‑проверки пройдены.

## Тестирование
- Юнит‑тесты: без изменений в коде ядра; прогонить существующие тесты `com.archimatetool.mcp/test`.
- Интеграционные/smoke: ручные шаги в PDE + `curl` проверки, как в шаге 6.

## Риски/роллбэк
- Риски: различия в поведении тулбара между версиями Eclipse/Archi; устаревшие настройки перспективы у пользователя могут скрывать новый тулбар.
- Митигации: использовать контейнер `org.eclipse.ui.main.toolbar` с `label="MCP"`, документировать включение через Customize Perspective.
- Роллбэк: вернуть прежнюю схему `locationURI`/откатить правки `plugin.xml`.


