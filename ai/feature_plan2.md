### План внедрения: экспорт изображения View (PNG/SVG) через REST + MCP (feature-plan 2)

Цель: добавить возможность получать изображение выбранного View как PNG или SVG через REST, и опубликовать это как MCP‑инструмент (возврат в base64), без UI‑диалогов.

—

Инварианты (см. `AGENTS.md`):
- Слушаем только `127.0.0.1:8765`.
- Любые операции, затрагивающие SWT/EMF, — через `Display.getDefault().syncExec(...)`.
- Не добавляем внешних зависимостей; используем экспортёр/утилиты Archi.
- Формат существующих ответов не меняем; новые маршруты совместимы.
- Источник правды для REST — `resources/openapi.json`. `archi-mcp-server/server.py` — источник описаний для LLM.

Процесс выполнения:
- Идём шагами 1 → …, каждый шаг — минимальные изменения + smoke‑проверки.
- Коммиты: `feat(view-image-step-01): ...`.

Smoke‑проверки (WSL):
```bash
VID="<view-id>"
curl -sS -o /tmp/v.png "http://127.0.0.1:8765/views/$VID/image?format=png&scale=1.25&bg=transparent" && file /tmp/v.png
curl -sS -o /tmp/v.svg "http://127.0.0.1:8765/views/$VID/image?format=svg&scale=1.0&bg=#ffffff&margin=8" && head -n 2 /tmp/v.svg
```

Критерии готовности:
- PNG и SVG отдаются с корректными `Content-Type` и отображаются внешними средствами.
- Параметры `format/scale/bg/margin/dpi` применяются.
- В MCP доступен инструмент, возвращающий base64 и content_type.

—

Шаг 1. Спецификация REST (OpenAPI)
- Добавить маршрут: `GET /views/{id}/image`.
- Параметры:
  - `format` — `png|svg`, по умолчанию `png`.
  - `scale` — число (0.1..4.0), по умолчанию `1.0`.
  - `dpi` — целое (только для PNG), опционально.
  - `bg` — `transparent` либо `#RRGGBB`.
  - `margin` — целое ≥ 0, по умолчанию 0.
- Ответы:
  - 200: `image/png` или `image/svg+xml; charset=utf-8` (тело — бинарные байты/текст SVG).
  - 400: неверные параметры; 404: view не найден; 409: нет активной модели.
- Критерий: путь и описания появятся в `openapi.json` консистентно с остальными.

Шаг 2. Хендлер `GET /views/{id}/image`
- В `ViewItemHttpHandler` добавить ветку `"image"` для метода GET.
- Разбор query‑параметров и валидации (границы `scale`, формат `bg`).
- Вызов из UI‑треда экспортёра, возврат бинарного тела и заголовка `Content-Type`.
- Ошибки → JSON `{"error":"..."}` с кодами 400/404/409 (через `ResponseUtil`).

Шаг 3. Экспорт в PNG (ModelApi)
- Добавить `byte[] renderViewToPNG(IDiagramModel view, float scale, Integer dpi, java.awt.Color bg, int margin)`.
- Реализация (варианты, выбрать доступный в Archi):
  - Использовать встроенные утилиты Archi (например, `DiagramUtils`/экспортёр изображений) для рендеринга в `Image`, затем `ImageLoader` → PNG.
  - Если нужен offscreen GC: создать `org.eclipse.swt.graphics.Image`, рисовать через GC, упаковать через `ImageLoader`.
- Учитывать `bg` (прозрачный фон для PNG при поддержке), `dpi` (если поддерживается), `margin`.
- Обязательно освобождать SWT‑ресурсы.

Шаг 4. Экспорт в SVG (ModelApi)
- Добавить `byte[] renderViewToSVG(IDiagramModel view, float scale, String bg, int margin)`.
- Использовать встроенный экспортёр SVG из Archi (например, `DiagramModelSVGExporter` или эквивалентную утилиту), получить `String` SVG и вернуть UTF‑8 байты.
- Учесть `bg` (прозрачный/заливка) и `scale`/`margin` (если поддерживается экспортёром).

Шаг 5. Интеграция в хендлер
- В ветке `image`: по `format` выбрать PNG/SVG путь, сформировать HTTP‑ответ с соответствующим `Content-Type` и телом.

Шаг 6. MCP‑инструмент
- В `archi-mcp-server/server.py` добавить:
  ```python
  def get_view_image(view_id: str, format: str = "png", scale: float = 1.0,
                     bg: str = "transparent", dpi: Optional[int] = None,
                     margin: int = 0) -> Dict[str, Any]
  ```
- Запрос к REST: `GET /views/{view_id}/image?...`.
- Ответ MCP: `{ "content_type": "image/png|image/svg+xml", "data_base64": "...", "bytes": <int> }`.
- Обновить `PARAM_DESCRIPTIONS` + короткий docstring.

Шаг 7. Документация
- `archi-mcp-plugin/README.md`: раздел «Экспорт изображения вида», параметры и примеры curl.
- `archi-mcp-plugin/AGENTS.md`: краткая памятка по параметрам и ограничениям.
- `archi-mcp-server/README.md`/`AGENTS.md`: описание MCP‑инструмента и пример сохранения файла из base64.

Шаг 8. Smoke‑тесты
- PNG: сохранить файл и открыть внешним просмотрщиком; проверить прозрачность/фон.
- SVG: проверить корректность структуры и отображения (в браузере).
- Большие виды: измерить время и память; при необходимости ограничить максимальный `scale`.

—

Роллбэк‑план
- Удалить ветку `image` из `ViewItemHttpHandler`, методы рендеринга из `ModelApi`, запись из `openapi.json`, MCP‑инструмент.

Риски/заметки
- Загрузка шрифтов/метрик при headless‑рендеринге; используем механизмы Archi.
- Прозрачность PNG зависит от используемого пути рендеринга; при проблемах — заполнять фон.
- Освобождение SWT‑ресурсов (Images/GC) обязательно, иначе утечки и артефакты.
- Ограничить `scale` (например, 0.1..4.0), чтобы не переполнить память на больших диаграммах.


