### План внедрения: автономная инструкция для агентов + ссылка из AGENTS.md (feature-plan 8)

Цель:
- Подготовить отдельную, OS‑агностичную инструкцию для автономных агентов (CI/контейнеры, Codex и пр.) по headless‑сборке и запуску/тестированию плагина.
- Добавить явную ссылку на эту инструкцию в самом начале `AGENTS.md`.

—

Инварианты (см. текущий `AGENTS.md` и состояние проекта):
- По умолчанию слушаем только `127.0.0.1:8765` (локально).
- Любые операции с EMF/SWT — через `Display.getDefault().syncExec(...)`.
- Источник правды по REST — `resources/openapi.json` (синхронизируем описания MCP‑инструментов при изменениях).
- Юнит‑тесты уже располагаются под `com.archimatetool.mcp/test` внутри плагина (JUnit без запуска Archi). Интеграционные тест‑данные (модели) пока отсутствуют — добавим при необходимости в отдельную папку.

Предупреждение (важно для автономных агентов):
- Некоторые агенты (например, Codex) не могут делать коммиты, если они содержат бинарные файлы. План и инструкция должны избегать необходимости добавлять/изменять бинарники в коммитах.
- Следствия: новые зависимости, иконки, тестовые модели и иные бинарные артефакты должны подключаться без включения самих бинарников в коммиты, либо обрабатываться отдельным (неагентным) шагом.

—

Шаг 1. Создать документ `AUTONOMOUS_AGENTS.md`
- Размещение: `archi-mcp-plugin/AUTONOMOUS_AGENTS.md`.
- Цель документа: пошагово описать, как агенту (в контейнере/CI) собрать и прогнать плагин без UI Archi.
- Структура разделов (скелет):
  1) Обзор и целевая аудитория (автономные агенты, CI, контейнеры)
  2) Требования окружения (JDK 17; без требований к WSL/GUI)
  3) Headless‑сборка (Maven Tycho + target platform Archi 5.x; офлайн‑режим — рекомендации по локальному p2‑репозиторию). Примечание: Tycho‑скрипты ещё не добавлены — вынести в отдельную фичу.
  4) Headless‑приложение MCP (CLI‑запуск Archi: `-application com.archimatetool.mcp.headless -nosplash -consoleLog`). Примечание: на данный момент в плагине отсутствует headless‑`Application`; требуется отдельная фича для его добавления.
  5) Конфигурация (переменные/свойства: `archi.mcp.port`, `ARCHI_MCP_PORT` и пр.)
  6) Тестовые данные (создать папку `com.archimatetool.mcp/testdata` и положить туда `.archimate` для интеграционных проверок)
  7) Интеграционные проверки (REST/MCP): примеры вызовов curl, сценарии smoke
  8) Логи и артефакты (stdout + файл лога; как сохранять в CI)
  9) Офлайн‑режим (как провизионить target platform и зависимости без интернета)
  10) Контейнер/CI (пример Dockerfile/скрипта; шаги pipeline)
  11) Troubleshooting (типовые ошибки биндинга порта, отсутствие активной модели и т.д.)

  Раздел «Ограничение на бинарные коммиты и стратегии обхода» (обязателен):
  - Зависимости/библиотеки: 
    - использовать Tycho/p2‑репозитории (Orbit/Maven Central OSGi bundles) — агент вносит только текстовые изменения (POM/target), бинарники подкачиваются на сборке;
    - для офлайн‑сборки — вынести бинарный зеркальный p2 в отдельный репозиторий/сабмодуль; агент обновляет только ссылку/ревизию субмодуля.
  - Медиа/иконки:
    - по возможности использовать SVG (текст) и генерировать/конвертировать в PNG на этапе сборки;
    - либо оставлять плейсхолдеры и фиксировать задачу на последующий ручной коммит бинарников.
  - Тестовые модели:
    - генерировать модель программно в тестах/скриптах (текстовые фикстуры JSON → создание через REST/MCP);
    - либо хранить в отдельном репозитории (субмодуль) и подтягивать в CI.
  - Git LFS (опционально): использовать, если инфраструктура проекта это поддерживает; агент коммитит текстовые LFS‑пойнтеры, а загрузка бинарников выполняется вне агента.

Шаг 2. Ссылка из `AGENTS.md`
- В начало `archi-mcp-plugin/AGENTS.md` вставить короткий блок «Если запускаете в автономной среде… см. `AUTONOMOUS_AGENTS.md`».
- Формулировка не должна менять существующие инварианты; лишь указывает альтернативный документ для автономных сценариев.

Шаг 3. Проверка предпосылок в коде/сборке
- Headless‑приложение в плагине отсутствует — нужна отдельная фича: добавить `org.eclipse.core.runtime.applications` → `com.archimatetool.mcp.headless` (класс `MCPHeadlessApplication`), стартующий `HttpServerRunner` без UI.
- Tycho‑сборка отсутствует — вынести в отдельную фичу: Tycho parent/p2 target, офлайн‑кеш.
- Интеграционные тест‑данные (модели) отсутствуют — создать `com.archimatetool.mcp/testdata` и положить пример.

Стратегии с учётом бинарного ограничения (внедрить параллельно):
- Перевести зависимости на p2/Tycho (Orbit) — убрать `lib/*.jar` из будущих изменений; не требуются бинарные коммиты.
- Для офлайна — добавить отдельный репозиторий `archi-mcp-binaries` (p2 mirror) и подключить как git‑submodule; агент меняет только ревизию субмодуля.
- Для тестовых моделей — предпочесть генерацию из текстовых фикстур; альтернативно — submodule.
- Для иконок — SVG в репо (текст), конверсия на сборке; новые PNG добавлять вручную (человеком) отдельным PR.

Шаг 4. Smoke‑проверки инструкции (OS‑агностично)
- В документ добавить минимальный набор команд:
  - Сборка (после добавления Tycho): `mvn -B -Dtycho.localArtifacts=ignore clean verify`
  - Запуск Archi headless c нашим appId и портом (после добавления headless‑Application; пример для Linux‑образа):
    ```bash
    /opt/archi/Archi -nosplash -consoleLog \
      -application com.archimatetool.mcp.headless \
      -Darchi.mcp.port=8765
    ```
  - REST‑smoke: `curl -sS http://127.0.0.1:8765/status | jq .`
- Описать ожидаемые результаты и коды возврата.

Шаг 5. Док‑ревью и вычитка
- Убедиться, что документ не ссылается на WSL/Windows‑специфику и подходит для «чистой» контейнерной среды.
- Согласовать терминологию и краткость с `AGENTS.md`.

Шаг 6. Критерии готовности (DoD)
- `AUTONOMOUS_AGENTS.md` добавлен, структурирован и самодостаточен.
- В `AGENTS.md` есть заметная ссылка в самом верху.
- Команды, зависящие от headless‑Application и Tycho, помечены как «после реализации»; базовые разделы применимы уже сейчас.

Шаг 7. Роллбэк
- Удалить файл `AUTONOMOUS_AGENTS.md` и блок ссылки из `AGENTS.md`.
- Остальная документация остаётся без изменений.


Приложение A. Tycho/p2/Orbit — минимальная инструкция (без бинарных коммитов)

- Цель: убрать `lib/*.jar` и собирать плагин против целевой платформы Eclipse/Archi, подтягивая бандлы из p2‑репозиториев (Orbit/Archi/зеркало).

- Структура Maven (пример):
  - root `pom.xml` (packaging `pom`) с Tycho и общими настройками
  - модуль `com.archimatetool.mcp` (packaging `eclipse-plugin`)
  - модуль `target-platform` (опционально, `eclipse-target-definition`), или inline target в root POM

- Плагины Tycho (root POM):
  - `org.eclipse.tycho:tycho-maven-plugin`
  - `org.eclipse.tycho:tycho-compiler-plugin`
  - `org.eclipse.tycho:target-platform-configuration`

- Target Platform (варианты):
  1) Отдельный `.target` файл с p2‑сайтом Eclipse + Orbit + локальный p2 Archi 5.x mirror.
  2) Inline в POM (коротко): указать p2‑репозитории в `target-platform-configuration`.

- P2 источники (примеры URL — заменить на актуальные):
  - Eclipse release: `https://download.eclipse.org/releases/2024-xx/`
  - Eclipse Orbit: `https://download.eclipse.org/tools/orbit/downloads/drops/R20240520194045/repository`
  - Локальный mirror Archi 5.x: `file:${project.basedir}/p2/archi-5.x` (см. «p2 mirror» ниже)

- Конфигурация target (inline‑пример):
```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>target-platform-configuration</artifactId>
  <version>${tycho.version}</version>
  <configuration>
    <environments>
      <environment>
        <os>linux</os><ws>gtk</ws><arch>x86_64</arch>
      </environment>
    </environments>
    <resolver>p2</resolver>
    <repositories>
      <repository>
        <id>eclipse-release</id>
        <layout>p2</layout>
        <url>https://download.eclipse.org/releases/2024-06/</url>
      </repository>
      <repository>
        <id>orbit</id>
        <layout>p2</layout>
        <url>https://download.eclipse.org/tools/orbit/downloads/drops/R20240520194045/repository</url>
      </repository>
      <repository>
        <id>archi-local</id>
        <layout>p2</layout>
        <url>file:${project.basedir}/p2/archi-5.x</url>
      </repository>
    </repositories>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>org.eclipse.tycho.extras</groupId>
      <artifactId>tycho-p2-extras-plugin</artifactId>
      <version>${tycho.extras.version}</version>
    </dependency>
  </dependencies>
</plugin>
```

- Требуемые импорты вместо `lib/*.jar`:
  - В `MANIFEST.MF` оставить `Require-Bundle`/`Import-Package` для нужных пакетов (например, `com.sun.net.httpserver` идёт из JDK; Jackson/другие — из Orbit при переходе).
  - Убрать `Bundle-ClassPath: lib/...` для внешних .jar, если замещены p2‑бандлами.

- P2 mirror для офлайна:
  - Создайте mirror нужных сайтов (Eclipse, Orbit, Archi 5.x) с помощью `tycho-p2-extras:mirror` или `p2.mirror` (Eclipse director).
  - Сохраните mirror в каталоге `p2/archi-5.x` (или внешнем репозитории/субмодуле) — в коммит идут только метаданные p2 (но обычно это тоже бинарные артефакты). Рекомендация: хранить mirror в отдельном репозитории (submodule), агент меняет только ревизию.

- Команда сборки:
  - Онлайн: `mvn -B -Dtycho.localArtifacts=ignore clean verify`
  - Офлайн (при наличии mirror и локального Maven repo): `mvn -B -o clean verify`

- Тестирование:
  - Текущие JUnit‑тесты из `com.archimatetool.mcp/test` продолжат выполняться как часть сборки плагина.
  - OSGi‑тесты можно добавить отдельным модулем `eclipse-test-plugin` и запускать через `tycho-surefire-plugin` (опционально).


