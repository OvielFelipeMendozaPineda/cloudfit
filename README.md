# cloud-fit

Hack day 2026 — a virtual closet that suggests outfits for an event.

Single Ktor app. The three concerns are just **packages** (folders), not separate services, so
there's one build and one deploy.

## Layout

```
src/main/kotlin/com/masabi/cloudfit/
├── Application.kt          Ktor bootstrap (server, JSON, CORS) · runs on :8080
├── models/                 domain types (Clothe, Outfit, ClothingCategory)
├── ai/                     Gemini client + clothe picker + image renderer
├── clothes/                API: closet CRUD, persistence (in-memory → RDS/S3 later)
├── outfit/                 orchestration (OutfitStylist) + /outfits route
└── storage/                image persistence (local disk → S3 later)
```

The routes call `OutfitStylist.compose(...)` **directly, in-process** — no HTTP hop
between the API and the AI code.

## Run locally

```bash
./gradlew run
```

Then:

```bash
curl -s localhost:8080/health
curl -s -X POST localhost:8080/clothes -H 'content-type: application/json' \
  -d '{"id":"","category":"TOP","imageUrl":"https://s3/shirt.png"}'
curl -s -X POST localhost:8080/outfits -H 'content-type: application/json' \
  -d '{"eventName":"brunch"}'
```

## Build & test

```bash
./gradlew build
```
