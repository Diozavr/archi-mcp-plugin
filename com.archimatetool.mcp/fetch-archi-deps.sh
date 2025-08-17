#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
LIB="$DIR/lib"
mkdir -p "$LIB"
URL_BASE="https://downloads.sourceforge.net/project/archi/Archi/5.6.0"
ARCHI_ZIP="$LIB/Archi-5.6.0.zip"
if [ ! -f "$ARCHI_ZIP" ]; then
  echo "Downloading Archi distribution..."
  curl -L "$URL_BASE/Archi-Linux64-5.6.0.zip" -o "$ARCHI_ZIP"
fi
if [ ! -f "$LIB/com.archimatetool.model.jar" ]; then
  echo "Extracting libraries..."
  unzip -q "$ARCHI_ZIP" "*/plugins/com.archimatetool.model_*.jar" -d "$LIB/tmp"
  unzip -q "$ARCHI_ZIP" "*/plugins/com.archimatetool.editor_*.jar" -d "$LIB/tmp"
  unzip -q "$ARCHI_ZIP" "*/plugins/com.archimatetool.export.svg_*.jar" -d "$LIB/tmp"
  unzip -q "$ARCHI_ZIP" "*/plugins/org.eclipse.draw2d_*.jar" -d "$LIB/tmp"
  unzip -q "$ARCHI_ZIP" "*/plugins/org.eclipse.gef_*.jar" -d "$LIB/tmp"
  mv "$LIB"/tmp/**/plugins/com.archimatetool.model_*.jar "$LIB/com.archimatetool.model.jar"
  mv "$LIB"/tmp/**/plugins/com.archimatetool.editor_*.jar "$LIB/com.archimatetool.editor.jar"
  mv "$LIB"/tmp/**/plugins/com.archimatetool.export.svg_*.jar "$LIB/com.archimatetool.export.svg.jar"
  mv "$LIB"/tmp/**/plugins/org.eclipse.draw2d_*.jar "$LIB/org.eclipse.draw2d.jar"
  mv "$LIB"/tmp/**/plugins/org.eclipse.gef_*.jar "$LIB/org.eclipse.gef.jar"
  rm -rf "$LIB/tmp"
fi
