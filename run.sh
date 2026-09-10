#!/usr/bin/env bash
# Loads env vars from .env (if present) and runs the app.
# Usage: ./run.sh   (pass extra gradle args after it, e.g. ./run.sh --no-daemon)
set -euo pipefail
cd "$(dirname "$0")"

if [ -f .env ]; then
  set -a          # auto-export everything sourced
  . ./.env
  set +a
fi

if [ -z "${GEMINI_API_KEY:-}" ]; then
  echo "error: GEMINI_API_KEY is not set. Copy .env.example to .env and add your key." >&2
  exit 1
fi

exec ./gradlew run "$@"
