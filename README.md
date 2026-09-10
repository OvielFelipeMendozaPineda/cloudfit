# cloud-fit

Hack day 2026 — a virtual closet that suggests outfits for an event.

Single Ktor app. The three concerns are just **packages** (folders), not separate services, so
there's one build and one deploy.

## Layout

```
src/main/kotlin/com/masabi/cloudfit/
├── Application.kt          Ktor bootstrap (server, JSON, CORS) · runs on :8080
├── shared/                 domain types (Clothe, Outfit, Event, ClothingCategory)
├── wardrobe/               API: closet CRUD, events, persistence (in-memory → RDS/S3 later)
└── outfit/                 the AI brain: OutfitStylist (pick garments → render image)
```

The wardrobe routes call `OutfitStylist.compose(...)` **directly, in-process** — no HTTP hop
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
curl -s -X POST localhost:8080/events -H 'content-type: application/json' \
  -d '{"id":"e1","name":"brunch"}'
curl -s -X POST localhost:8080/events/e1/outfit
```

## Build & test

```bash
./gradlew build
```

## The AI part (Felipe) — where to plug the real models

`outfit/OutfitStylist.kt` has two TODOs:
- `pickGarments` → LLM that "sees" the garment images + the event and picks the outfit.
- `renderImage` → image model (Gemini/etc.) that renders the avatar wearing it, then uploads to S3.
