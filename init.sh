#!/usr/bin/env bash
set -euo pipefail

# Windows is the primary environment for this repository. This wrapper keeps the
# conventional harness entry point while delegating to the safer PowerShell runner.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if command -v pwsh >/dev/null 2>&1; then
  exec pwsh -NoProfile -ExecutionPolicy Bypass -File "$ROOT_DIR/init.ps1" "$@"
fi

if command -v powershell.exe >/dev/null 2>&1; then
  exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ROOT_DIR/init.ps1" "$@"
fi

if command -v powershell >/dev/null 2>&1; then
  exec powershell -NoProfile -ExecutionPolicy Bypass -File "$ROOT_DIR/init.ps1" "$@"
fi

echo "PowerShell is required to run this project harness." >&2
exit 127
