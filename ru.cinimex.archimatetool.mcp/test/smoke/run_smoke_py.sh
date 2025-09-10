#!/usr/bin/env bash
set -euo pipefail

# Run Python smoke test from WSL using Windows Python so localhost works.

# Resolve script directory and python test path
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PY_FILE_LNX="${SCRIPT_DIR}/test_smoke.py"

# Convert to Windows path for python.exe
if ! command -v wslpath >/dev/null 2>&1; then
  echo "wslpath not found; this launcher must run inside WSL" >&2
  exit 1
fi
PY_FILE_WIN="$(wslpath -w "$PY_FILE_LNX")"

# Pick Windows Python
if command -v python.exe >/dev/null 2>&1; then
  PYEXE="python.exe"
elif command -v py.exe >/dev/null 2>&1; then
  PYEXE="py.exe -3"
else
  echo "Windows Python (python.exe or py.exe) not found in PATH" >&2
  exit 1
fi

# Ensure localhost host/port env (can be overridden by caller)
export ARCHI_MCP_HOST="${ARCHI_MCP_HOST:-127.0.0.1}"
export ARCHI_MCP_PORT="${ARCHI_MCP_PORT:-8765}"

# Optional debug
if [ "${DEBUG:-}" = "1" ]; then
  set -x
fi

exec $PYEXE "$PY_FILE_WIN"


