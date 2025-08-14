### План внедрения: настройки плагина в Eclipse Preferences (порт) — feature-plan 6

Цель:
- Добавить страницу настроек плагина в стандартные Eclipse Preferences (Archi → Preferences), позволяющую задавать порт HTTP‑сервера MCP/REST.
- Порт применяется без перезапуска Archi: при изменении — авто‑перезапуск встроенного сервера.

—

Инварианты и совместимость:
- Слушаем только `127.0.0.1`.
- Приоритет источников порта (не ломаем существующее):
  1) System Property `archi.mcp.port`
  2) Env `ARCHI_MCP_PORT`
  3) Plugin Preferences `com.archimatetool.mcp:port`
  4) Default: `8765`
- Любые операции с EMF/SWT — через `Display.getDefault().syncExec(...)`.

—

Шаг 1. Ключи и хранилище настроек
- Ввести константы ключей, напр.: `public static final String PREF_PORT = "port";` в `com.archimatetool.mcp.preferences` (новый `MCPPreferences.java`).
- Использовать `IEclipsePreferences` (`InstanceScope.INSTANCE.getNode("com.archimatetool.mcp")`).
- Создать `MCPPreferenceInitializer` (extends `AbstractPreferenceInitializer`) и прописать дефолт: `port=8765`.
- Статус: [done]
- Сделано: введены `MCPPreferences` и `MCPPreferenceInitializer` с дефолтом порта 8765.
- Осталось: —

Шаг 2. Страница Preferences (UI)
- Изменить `MCPPreferencePage` на `FieldEditorPreferencePage` (тип: `GRID`) для встроенной валидации.
- Добавить `StringFieldEditor` для `host`:
  - Дефолт: `127.0.0.1`.
  - Валидация: разрешать как минимум `127.0.0.1` и `localhost`. Для других значений показывать предупреждение (Label) о рисках
  - Тултип: «Effective precedence: System Property → Env → Preferences → Default».
- Добавить `IntegerFieldEditor` для `port`:
  - Диапазон 1024..65535 (или 1..65535, но рекомендовать 1024+).
  - Значение по умолчанию 8765.
  - Подпись: «Port (localhost only)»; тултип: «Effective precedence: System Property → Env → Preferences → Default».
- Показать текущий эффективный порт (read‑only label) с учётом приоритета (System/Env могут переопределять Preferences).
- Optionally: кнопка «Restart server now» (необязательно, т.к. автоперезапуск будет в Apply/OK).
- Статус: [done]
- Сделано: `MCPPreferencePage` переведена на `FieldEditorPreferencePage`, добавлены поля host/port с предупреждением для нелокального хоста и меткой эффективного порта.
- Осталось: реализовать необязательную кнопку ручного перезапуска при необходимости.

Шаг 3. Применение и автоперезапуск
- В `MCPPreferencePage.performOk()`/`performApply()`:
  - Определить старый порт (текущий, на котором слушает сервер) и новый порт из Preferences.
  - Если изменился и сервер запущен — вызвать перезапуск сервера: `Activator.getDefault().restartServer(newPort)`.
- Добавить в `Activator`/`HttpServerRunner` методы:
  - `public int getBoundPort()` — вернуть фактически занятый порт.
  - `public synchronized void restart()` — `stop()` → `start()` (учитывает новый `Config.resolvePort()`).
  - В `Activator`: `public void restartServer()` обёртка с нотификацией статуса (лог/label).
- Статус: [done]
- Сделано: реализованы перезапуск сервера при смене порта и методы `getBoundPort`/`restart`.
- Осталось: —

Шаг 4. Чтение настроек в конфиге
- Обновить `Config.resolvePort()`:
  - Сначала проверить System Property и Env (как сейчас).
  - Затем попробовать прочитать `InstanceScope` → `com.archimatetool.mcp` → `port` (int, безопасный парсинг, границы).
  - Fallback: `DEFAULT_PORT`.
- Не создавать жёсткой зависимости от `Activator`: использовать `IEclipsePreferences` напрямую, чтобы вызов работал в момент старта бандла.
- Статус: [done]
- Сделано: `Config.resolvePort()` учитывает приоритет System→Env→Prefs→Default без зависимости от `Activator`.
- Осталось: —

Шаг 5. Валидация и UX‑детали
- При невалидном значении `port` — блокировать `OK/Apply` (поведение `IntegerFieldEditor`).
- Если перезапуск не удался (порт занят):
  - Показать диалог/ошибку на странице и откатить поле к прежнему значению или оставить поле, но вернуть прежний порт на уровне рантайма; логировать причину.
  - В лог‑окно (см. отдельный план для логов) записать stacktrace/ошибку биндинга.
- Статус: [partial]
- Сделано: базовая валидация полей и предупреждение для нелокального хоста.
- Осталось: обработка ошибок занятости порта и логирование.

Шаг 6. Тесты
- Юнит: `Config.resolvePort()` — сценарии приоритета (System/Env/Prefs/Default).
- Юнит: `MCPPreferenceInitializer` — дефолты применяются.
- Интеграционный (эмуляция): сохранить порт в prefs → вызвать `restart()` → проверить, что сервер слушает новый порт (через `getBoundPort()` или попытку `curl`).
- Статус: [partial]
- Сделано: добавлены юнит‑тесты `ConfigTest` и `MCPPreferenceInitializerTest`.
- Осталось: интеграционный сценарий перезапуска.

Шаг 7. Smoke‑проверки (WSL)
```bash
# 1) Проверка дефолта
curl -sS http://127.0.0.1:8765/status | jq .

# 2) Изменить порт в Preferences на 8081 → Apply/OK (автоперезапуск)
curl -sS http://127.0.0.1:8081/status | jq .

# 3) Вернуть 8765
curl -sS http://127.0.0.1:8765/status | jq .
```
- Статус: [todo]
- Сделано: —
- Осталось: выполнить smoke‑проверки по инструкции.

Шаг 8. Документация
- `README.md`: краткая инструкция по настройке порта и приоритету источников.
- `AGENTS.md`: пункт «конфигурация» — отразить новый слой Preferences.
- Статус: [done]
- Сделано: обновлены `README.md` и `AGENTS.md` с описанием настроек и приоритета источников.
- Осталось: —

Шаг 9. Роллбэк
- Удалить `MCPPreferenceInitializer`, правки `MCPPreferencePage` и чтение из Preferences в `Config.resolvePort()`.
- Сервер вернётся к управлению портом через System/Env/Default.
- Статус: [todo]
- Сделано: —
- Осталось: описать и реализовать процедуру отката при необходимости.

Критерии готовности (DoD)
- Страница Preferences отображает поле порта с валидацией и «effective port». 
- Изменение порта приводит к перезапуску сервера без перезапуска Archi.
- При занятости порта отображается понятная ошибка, старый порт продолжает работать.
- Тесты зелёные, smoke‑проверки проходят.


