## Сборка плагина в Eclipse (PDE)

Этот документ описывает, как собрать и установить плагин `ru.cinimex.archimatetool.mcp` для Archi 5.x с помощью Eclipse PDE.

### Требования
- **Eclipse**: Eclipse IDE for RCP and RAP Developers (2023‑12 или новее) с PDE.
- **JDK**: Java 17 (Temurin/OpenJDK). В Eclipse выставьте JavaSE‑17 как дефолтный JRE.
- **Archi**: установленный Archi 5.x. Понадобится путь к его папке `plugins`.

### Импорт проекта
1. File → Import → Existing Projects into Workspace.
2. Укажите каталог `archi-mcp-plugin` и импортируйте проект `ru.cinimex.archimatetool.mcp`.
3. Project → Properties → Java Compiler → выставьте **17**.

### Target Platform (Archi 5.x)
1. Preferences → Plug‑in Development → Target Platform → Add…
2. Выберите “Nothing: Start with an empty target” → Next.
3. Add → Directory → укажите папку `plugins` установленного Archi (например, `C:\Program Files\Archi\plugins` или путь к распакованному дистрибутиву Archi).
4. Name: “Archi 5.x” → Finish → отметьте как Active → Apply & Close.

Проверьте, что зависимости в `META-INF/MANIFEST.MF` (вкладка Dependencies) резолвятся: `com.archimatetool.editor`, `com.archimatetool.model`, `org.eclipse.*` и т.д.

### Сборка плагина (PDE Export)
1. Project → Clean.
2. File → Export → Plug‑in Development → Deployable plug‑ins and fragments.
3. Отметьте `ru.cinimex.archimatetool.mcp`.
4. Destination:
   - Выберите Archive file и путь для `ru.cinimex.archimatetool.mcp_<version>.zip`.
   - Снимите «Include required plug‑ins» (не нужен полный набор Archi).
   - «Use class files» — по умолчанию подходит.
5. Finish.

Файл `build.properties` уже включает необходимые ресурсы (`lib/`, `img/`, `resources/`) и jackson‑jars, поэтому они попадут в итоговый ZIP.

### Установка в Archi
- Поместите полученный ZIP в папку `dropins` установленного Archi (например, `C:\Program Files\Archi\dropins`) и перезапустите Archi.
- Альтернатива: распакуйте содержимое ZIP в подкаталог `dropins\ru.cinimex.archimatetool.mcp`.

### Запуск/отладка из Eclipse (опционально)
1. Run → Run Configurations… → Eclipse Application → New.
2. Выберите «Run an application: `com.archimatetool.editor.product`» (если доступен продукт Archi в Target Platform).
3. VM arguments (опционально): `-Darchi.mcp.host=127.0.0.1 -Darchi.mcp.port=8765`.
4. Run. В Archi появится тулбар «MCP» — переключатель «MCP Server».

### Быстрые проверки
- Переключите «MCP Server» на тулбаре, запустите сервер.
- Проверьте `http://127.0.0.1:8765/status` — ожидается `{"ok":true,...}`.
- Если установлен jArchi, доступны REST‑маршруты скриптов.

### Примечания
- Хост и порт настраиваются: System Properties `archi.mcp.host`/`archi.mcp.port`, Env `ARCHI_MCP_HOST`/`ARCHI_MCP_PORT` или Preferences (Archi → Preferences → MCP). Приоритет: System Property → Env → Preferences → Default (`127.0.0.1:8765`).
- Зависимость `com.archimatetool.script` помечена как optional — без jArchi скриптовый функционал будет отключён, остальное работает.
- Требуется Java 17 (`Bundle-RequiredExecutionEnvironment: JavaSE-17`).

### Частые проблемы
- Ошибки в проекте: Сделайте Clean Project. 
- «Unresolved bundle»/красные зависимости: активируйте Target Platform, укажите правильную папку `plugins` Archi 5.x.
- Неверная версия JRE: проверьте, что проект компилируется под 17 и установлен JDK 17.
- Порт занят: измените порт через VM аргумент или Preferences.


