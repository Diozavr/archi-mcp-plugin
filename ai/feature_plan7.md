### План внедрения: кнопка на панели Archi для запуска/остановки MCP с индикацией состояния — feature-plan 7

Цель:
- Добавить кнопку на главную панель инструментов Archi для управления сервером MCP/REST (start/stop) и отображения текущего состояния (запущен/остановлен), без перезапуска Archi.

—

Инварианты:
- Сервер слушает только `127.0.0.1` и порт, определённый через `Config.resolvePort()` (приоритеты System→Env→Prefs→Default, см. feature-plan 6).
- Операции, влияющие на EMF/SWT, выполняются через `Display.getDefault().syncExec(...)`.

—

Шаг 1. Расширение API управления сервером
- В `Activator` добавить:
  - `public synchronized void startServer()` — запускает, если не запущен.
  - `public synchronized void stopServer()` — останавливает, если запущен.
  - `public synchronized void restartServer()` — безопасно перезапускает.
  - `public synchronized boolean isServerRunning()` — признак активного `HttpServerRunner`.
  - `public synchronized int getBoundPort()` — фактически занятый порт (или `-1`, если не запущен).
- В `HttpServerRunner` добавить:
  - `public boolean isRunning()`
  - `public int getPort()` — сохранение порта при `start()`.
  - Защитить методы `start/stop` от повторных вызовов; выбрасывать информативные `IOException`/`IllegalStateException`.
- Инициализация: оставить автозапуск на старте (или сделать управляемым настройкой, см. feature-plan 6).

Шаг 2. Команда и обработчик
- В `plugin.xml` определить:
  - `org.eclipse.ui.commands`: команда `com.archimatetool.mcp.commands.toggleServer` (Label: "MCP Server", Category: "Archi MCP").
  - `org.eclipse.ui.handlers`: хендлер `com.archimatetool.mcp.ui.ToggleServerHandler` для команды.
- Реализация `ToggleServerHandler`:
  - Имплементировать `IHandler` и `IElementUpdater`.
  - `execute(...)`: если `Activator.isServerRunning()` → `stopServer()`; иначе `startServer()`; при ошибке — показать `MessageDialog.openError(...)`.
  - `updateElement(UIElement, Map)` — обновлять `checked`, `text`, `tooltip`, `icon` в зависимости от состояния.
  - Логи писать через `ILog` бандла.

Шаг 3. Вклад в главную панель инструментов
- В `plugin.xml` добавить `org.eclipse.ui.menus`:
  - `menuContribution` `locationURI="toolbar:org.eclipse.ui.main.toolbar"`.
  - Элемент `command` с `commandId="com.archimatetool.mcp.commands.toggleServer"`, `style="toggle"`.
  - Начальный `icon` — состояние по умолчанию (будет динамически изменяться в `updateElement`).
  - `tooltip`: например, "Start/Stop MCP Server (127.0.0.1:<port>)" с подстановкой порта в `updateElement`.

Шаг 4. Иконки и ресурсы
- Добавить в `img/`: `mcp_on.png`, `mcp_on@2x.png`, `mcp_off.png`, `mcp_off@2x.png`.
- Обновить `build.properties` → `bin.includes` для `img/`.
- В `ToggleServerHandler.updateElement` переключать иконку между on/off.

Шаг 5. Интеграция с настройками
- Если реализована настройка "автозапуск" (опционально):
  - При старте плагина запускать сервер только при `autoStart=true`.
  - Кнопка всегда доступна; включает/выключает вне зависимости от автозапуска.
- Отражать фактический порт из `Activator.getBoundPort()` в тултипе.

Шаг 6. Событийная синхронизация
- После `start/stop/restart` уведомлять UI о смене состояния:
  - Вызвать `IServiceLocator` → `ICommandService.refreshElements("com.archimatetool.mcp.commands.toggleServer", null)` для обновления иконки/состояния кнопки.
  - Дополнительно, если есть лог‑view (см. отдельный план), писать строку "Started at 127.0.0.1:<port>"/"Stopped".

Шаг 7. Обработка ошибок
- Если при старте порт занят — показать понятное сообщение ("Port in use: <port>") и оставить состояние "остановлен".
- Исключения логировать в `ILog` и (при наличии) в окно логов.

Шаг 8. Тесты и smoke
- Юнит (минимальный):
  - `Activator.isServerRunning()` корректно отражает состояние после вызовов `start/stop` (замокать биндинг).
- Ручной smoke (WSL):
  1) Запустить Archi: кнопка отображает "ON"/иконку включено (если автозапуск включен) или "OFF".
  2) Нажать: проверить `curl -sS http://127.0.0.1:<port>/status` → 200/ошибка соответственно.
  3) Изменить порт в Preferences → Apply: кнопка остаётся в согласованном состоянии; при следующем старте — отражает новый порт.

Шаг 9. Документация
- `README.md`: добавить раздел "Toolbar (MCP Server)" с описанием поведения, иконок и ошибок биндинга.
- `AGENTS.md`: кратко описать управление сервером через кнопку.

Шаг 10. Роллбэк
- Удалить команду/хендлер и вклад `org.eclipse.ui.menus` из `plugin.xml`, классы `ToggleServerHandler`, иконки.
- Функционал REST/MCP остаётся без изменений.

Критерии готовности (DoD)
- Кнопка на панели присутствует, корректно отражает состояние (checked + иконка on/off).
- Нажатие стартует/останавливает сервер; ошибки отображаются пользователю.
- Тултип показывает актуальный порт; UI синхронизируется после смены состояния.


