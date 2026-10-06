# CloudFit API

## Stack
- **Runtime**: Kotlin 2.1.21, JVM 21
- **Framework**: Ktor 3.1.3 (Netty, EngineMain, `application.yaml` con `$ENV:default`)
- **DB**: PostgreSQL + HikariCP + Exposed 0.61 (`exposed-java-time`, `timestamptz`, `uuid`)
- **Migraciones**: Flyway, `src/main/resources/db/migration` (se aplican al arrancar). Nunca DDL suelto ni `SchemaUtils`.
- **Auth**: JWT HS256 propio (15 min, `kid`), refresh opaco rotativo en cookie `cf_refresh`, Argon2id (password4j), Google/Apple vía JWKS
- **IA**: Gemini REST (`GeminiClient` implementa `TextModel`/`ImageModel`), uso de tokens en `ai_usage`
- **Pagos**: Stripe (REST con Ktor client), Wompi (web checkout), modo `fake` para dev
- **Tests**: Kotest + fakes en memoria, Testcontainers opcional, Kover ≥80% en `application.usecase`
- **Build**: Gradle 9 (wrapper), version catalog `gradle/libs.versions.toml`

## Arquitectura
```
src/main/kotlin/app/cloudfit/   Application.kt + wiring/ (adapters entre módulos) + config/
shared-code/                    AppException, StatusPages, JWT, rate limits, dbQuery/TransactionRunner, IA, ImageStore
accounts-service/               auth + /me
wardrobe-service/               clothes, avatar, uploads, remove-bg, tagger, defaults, images
styling-service/                looks async (picker + renderer)
billing-service/                créditos, catálogo, checkout, webhooks, ads
```
Cada módulo: `domain/`, `application/port/input`, `application/port/output`, `application/usecase`,
`infrastructure/adapter/input/http`, `infrastructure/adapter/output/*`, `infrastructure/config/*Module.kt`.
Los módulos solo dependen de `shared-code`; las dependencias cruzadas son output ports conectados en `app.cloudfit.wiring`.

## API
- Contrato: `docs/api-contract.md` — cualquier cambio de ruta/shape/código se documenta ahí primero.
- Base `/api/v1`, errores `{ "error": CODE, "message": ... }` lanzando subclases de `AppException`.
- `GET /health` y `/api/v1/health`.

## Comandos
```bash
./run.sh                         # dev local (lee .env)
./gradlew build                  # compila + tests + koverVerify
./gradlew :billing-service:test  # tests de un módulo
./gradlew installDist            # distribución (Dockerfile)
```

## Convenciones
- Sin comentarios salvo necesarios. Una responsabilidad por clase. DTOs HTTP con sufijo `Dto`.
- Casos de uso: interfaz en `port/input`, implementación `*Service` en `usecase`.
- Toda consulta de datos de usuario filtra por `user_id` del token (`call.requireAuthenticatedUser()`).
- Repos usan `dbQuery {}` (reusa la transacción actual si existe). Atomicidad entre repos/módulos con `TransactionRunner.inTransaction {}`.
- Errores nuevos: añadir subclase/código en `shared-code/.../AppException.kt` + mapping en `StatusPages.kt` + contrato.

## Trampas
1. **Créditos**: siempre vía `CreditBook` dentro de una transacción (bloquea buckets con `FOR UPDATE`). El ledger es append-only (trigger). Idempotencia por `idempotency_key` único.
2. **Looks**: la reserva del crédito y el INSERT del look van en la misma transacción. El job corre en el `CoroutineScope` de la app (no sobrevive reinicios: el sweeper los marca `INTERRUPTED` y reembolsa a los 10 min).
3. **Webhooks**: los créditos de compras solo se otorgan con webhook verificado (o en `PAYMENTS_MODE=fake`).
4. **Imágenes**: `IMAGE_STORE=local` sirve `/api/v1/images/{key}`; el renderer solo lee imágenes del propio store.
5. **Secretos**: nunca leer ni imprimir `.env`; solo `.env.example`. `JWT_SECRET` < 32 bytes → la app no arranca.
6. **Tests**: nunca llamar a Gemini/Stripe/Wompi/remove.bg reales. `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` se fija en el task `test` raíz (Colima).
7. **Precios** del catálogo en `application.yaml` son PLACEHOLDER.
