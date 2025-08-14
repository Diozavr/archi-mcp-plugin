### План: Maven-сборка JAR и запуск unit-тестов для плагина (feature-plan 8.1)

## Цель
- Настроить `pom.xml` и окружение так, чтобы:
  - можно было собрать JAR артефакт модуля `com.archimatetool.mcp` командой `mvn package`;
  - запускать unit‑тесты через `mvn test` (headless), без требований к установленному Archi.

## Контекст/Предпосылки
- Код — Eclipse плагин (OSGi) с собственным `META-INF/MANIFEST.MF`, ресурсами (`plugin.xml`, `resources/openapi.json`, `img/`, `lib/`).
- Тесты расположены под `com.archimatetool.mcp/test` (указаны в текущем `pom.xml` как `<testSourceDirectory>test</testSourceDirectory>`).
- Часть тестов не требует среду Archi/SWT (например, `JsonReaderTest`), но возможны тесты, зависимые от EMF/SWT.

## Инварианты
- Java 17 (`<release>17</release>` / `maven-compiler-plugin`).
- Тесты по умолчанию headless; тесты, требующие SWT/Archi, должны быть либо замоканы, либо исключены/помечены профилем.
- В JAR должны попасть скомпилированные классы и необходимые runtime‑ресурсы (минимум: `META-INF/MANIFEST.MF`, `plugin.xml`, `resources/**`).

## Целевая архитектура
- Packaging: `jar` с включением OSGi манифеста из исходников (без пере‑генерации m-bundle‑plugin).
- Unit‑тесты: JUnit 4 (существующие тесты), запуск `maven-surefire-plugin`.
- Ресурсы: `maven-resources-plugin` копирует статические ресурсы в `target/classes`.

## Предлагаемые изменения/добавления в pom.xml
- Свойства/энкодинг:
  - `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`
  - `<maven.compiler.release>17</maven.compiler.release>`
- Зависимости тестов:
  - `junit:junit:4.13.2`
  - `org.hamcrest:hamcrest:2.2` (или использовать транзитивный hamcrest-core от junit)
- Плагины:
  - `maven-compiler-plugin` (release=17)
  - `maven-surefire-plugin` с `includes` = `**/*Test.java`
  - `maven-resources-plugin` — ресурсы: `META-INF/**`, `plugin.xml`, `resources/**`, `img/**` (по необходимости)
  - `maven-jar-plugin` — упаковка существующего `META-INF/MANIFEST.MF` и исключение тестовых ресурсов
- Профили:
  - `with-swt` (опционально): подключает зависимости SWT/Archi для интеграционных тестов, по умолчанию выключен.

## Маппинг интерфейса → ядро/сервисы (примеры)
- Не требуется. Сборочный план.

## Политика ошибок (если применимо)
- При отсутствии SWT/Archi зависимости — соответствующие тесты исключены/помечены `@Ignore`/суффиксом `IT` и запускаются отдельным профилем.

## Потоковая модель (если применимо)
- Не влияет на сборку. Тесты, требующие UI‑тред, исключаются из дефолтного профиля.

## Валидация/парсинг (если применимо)
- N/A.

## Шаги реализации
1) Базовая конфигурация Maven
   - Добавить свойства `sourceEncoding=UTF-8`, `maven.compiler.release=17`.
   - Убедиться, что `<sourceDirectory>src</sourceDirectory>` и `<testSourceDirectory>test</testSourceDirectory>` корректны.
   - Статус: [todo]
   - Сделано: —
   - Осталось: правка `pom.xml`
2) Зависимости тестов
   - Добавить `junit:junit:4.13.2:test`, `org.hamcrest:hamcrest:2.2:test`.
   - Статус: [todo]
   - Сделано: —
   - Осталось: правка `pom.xml`
3) Плагины сборки
   - `maven-compiler-plugin` (3.13.0) — `release=17` (уже есть, проверить свойства/дубли).
   - `maven-surefire-plugin` — конфигурация `includes`, `trimStackTrace`, `failIfNoTests=false`.
   - `maven-resources-plugin` — добавить ресурсы: `META-INF/**`, `plugin.xml`, `resources/**`.
   - `maven-jar-plugin` — `archive.manifestFile = src/main/resources/META-INF/MANIFEST.MF` ИЛИ использовать существующий `META-INF/MANIFEST.MF` в исходниках (если хранится под `com.archimatetool.mcp/META-INF`).
   - Статус: [todo]
   - Сделано: —
   - Осталось: правка `pom.xml` и, при необходимости, перенос `MANIFEST.MF` в ресурсы
4) Исключение интеграционных тестов (если есть)
   - В дефолтном профиле исключить тесты, требующие SWT/Archi, через шаблоны `excludes`.
   - Ввести профиль `with-swt` (опционально) для их запуска (зависимости и VM‑опции).
   - Статус: [todo]
   - Сделано: —
   - Осталось: правка `pom.xml`
5) Копирование артефактов плагина
   - Убедиться, что в JAR попали `plugin.xml`, `META-INF/MANIFEST.MF`, `resources/openapi.json` (и другие необходимые ресурсы), но не попали тестовые файлы.
   - Статус: [todo]
   - Сделано: —
   - Осталось: настройка `resources`/`jar` плагина
6) Проверка локально
   - `mvn -q -DskipTests package` — быстрый прогон упаковки.
   - `mvn -q test` — запуск юнит‑тестов.
   - Статус: [todo]
   - Сделано: —
   - Осталось: выполнить команды и зафиксировать результат

## Ведение прогресса по шагам
- Для каждого шага поддерживать блоки: Статус/Сделано/Осталось; обновлять при правках `pom.xml` и при успешном прогоне `mvn`.

## DoD (Definition of Done)
- Команда `mvn package` создаёт JAR (`target/com.archimatetool.mcp-<version>.jar`) с манифестом и ресурсами.
- Команда `mvn test` выполняет unit‑тесты и завершаетcя успешно (0 failures); тесты, требующие SWT/Archi, не ломают сборку по умолчанию.
- Документация в `README.md`/`AGENTS.md` кратко описывает, как запустить сборку и тесты.

## Тестирование
- Юнит‑тесты: `JsonReaderTest`, утилиты (`StringCaseUtil`, `JsonUtil`), core‑методы, не требующие SWT/Archi.
- Интеграционные (опциональный профиль): тесты, зависящие от EMF/SWT/Archi.

## Риски/роллбэк
- Риски: попадание лишних файлов в JAR; зависимость тестов от SWT/Archi ломает CI.
- Роллбэк: отключить проблемные тесты через excludes/профили; вернуть минимальную конфигурацию `pom.xml` с только `compiler`/`surefire`.


