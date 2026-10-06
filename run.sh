#!/usr/bin/env bash
# Loads env vars from .env (if present) and runs the API with Gradle (no Docker).
# Usage: ./run.sh   (extra gradle args go after it, e.g. ./run.sh --no-daemon)
# Needs a reachable PostgreSQL (DB_*) and JWT_SECRET (>= 32 bytes).
set -euo pipefail
cd "$(dirname "$0")"

if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

if [ -z "${JWT_SECRET:-}" ]; then
  echo "error: JWT_SECRET is not set (>= 32 bytes). Copy .env.example to .env and run: openssl rand -hex 32" >&2
  exit 1
fi

if [ -z "${GEMINI_API_KEY:-}" ]; then
  echo "warning: GEMINI_API_KEY is not set — tagging and looks will fail with AI_UNAVAILABLE." >&2
fi

exec ./gradlew run "$@"
