# cloud-fit

API de CloudFit: armario virtual + estilista con IA (Gemini) + créditos y pagos.
Kotlin 2.1 · Ktor 3 · PostgreSQL (Exposed + Flyway) · arquitectura hexagonal multi-módulo.

Contrato con el front: [`docs/api-contract.md`](docs/api-contract.md) (fuente de verdad).

## Arquitectura

```
cloud-fit/
├── src/main/kotlin/app/cloudfit/     módulo raíz: Application.kt (plugins + rutas) y wiring/ (adapters entre módulos)
│   └── resources/db/migration/       migraciones Flyway (V1__init.sql)
├── shared-code/                      errores (AppException), StatusPages, JWT, rate limits, dbQuery/TransactionRunner,
│                                     puertos de IA (TextModel/ImageModel + GeminiClient), ImageStore (local/S3)
├── accounts-service/                 registro, verificación, login, Google/Apple, refresh rotativo, /me
├── wardrobe-service/                 prendas, avatar, uploads, remove.bg, tagger, defaults, /images/{key}
├── styling-service/                  looks asíncronos (picker + renderer) con reserva de créditos
└── billing-service/                  ledger de créditos, catálogo, checkout Stripe/Wompi/fake, webhooks, ads reward
```

Cada módulo sigue el mismo esquema:

```
domain/                                   modelos puros
application/port/input/                   interfaces de casos de uso
application/port/output/                  puertos hacia afuera (repos, proveedores, otros módulos)
application/usecase/                      implementaciones (*Service)
infrastructure/adapter/input/http/        rutas Ktor + DTOs (*Dto)
infrastructure/adapter/output/...         Postgres, Gemini, Stripe, Wompi, SMTP, JWKS...
infrastructure/config/*Module.kt          *Components: arma el módulo y registra rutas
```

Los módulos no se conocen entre sí: cada uno declara lo que necesita como output port
(p. ej. styling define `CreditWallet` y `WardrobeReader`) y `src/main/kotlin/app/cloudfit/wiring/`
los conecta con los casos de uso del otro módulo.

### Créditos

- `credit_ledger` append-only (trigger que impide UPDATE) + `wallet_buckets` (PLAN, FREE, REWARD, PACK).
- Toda operación bloquea los buckets del usuario (`SELECT … FOR UPDATE`) dentro de una transacción.
- Consumo: PLAN → FREE → REWARD → PACK. El reembolso vuelve a los mismos buckets.
- Idempotencia por `idempotency_key` único (`signup:<userId>`, `stripe:checkout:<session>`, `wompi:<tx>`, …).
- `POST /looks` reserva el crédito y crea el look en la misma transacción; READY confirma, FAILED reembolsa.
  Un sweeper marca como `FAILED/INTERRUPTED` (+ reembolso) los looks colgados más de 10 min.

## Correr local

Requisitos: JDK 21 y un PostgreSQL.

```bash
docker run -d --name cloudfit-pg -e POSTGRES_PASSWORD=cloudfit -e POSTGRES_USER=cloudfit \
  -e POSTGRES_DB=cloudfit -p 5432:5432 postgres:17-alpine
cp .env.example .env        # completa DB_PASSWORD=cloudfit, JWT_SECRET=$(openssl rand -hex 32), GEMINI_API_KEY
./run.sh                    # Flyway migra al arrancar; API en :8080
```

Con `APP_ENV=dev`: CORS para `http://localhost:5173`, `PAYMENTS_MODE=fake` (el checkout acredita al instante)
y, sin `SMTP_HOST`, los emails de verificación/reset se imprimen en el log (`[DEV EMAIL]`).

```bash
curl -s localhost:8080/health
curl -s -X POST localhost:8080/api/v1/auth/register -H 'content-type: application/json' \
  -d '{"email":"ana@example.com","password":"supersecret1","locale":"es"}'
# copia el token del log y:
curl -s -X POST localhost:8080/api/v1/auth/verify-email -H 'content-type: application/json' -d '{"token":"<token>"}'
```

### Docker

```bash
docker build -t cloudfit-api .
docker run --env-file .env -e DB_HOST=host.docker.internal -p 8080:8080 -v cloudfit-images:/data/images cloudfit-api
```

Imagen `eclipse-temurin:21-jre-alpine`, `JAVA_OPTS="-Xmx256m -XX:+UseSerialGC"`, healthcheck en `/health`.

## Build y tests

```bash
./gradlew build          # compila, tests (Kotest) y koverVerify (≥80% de líneas en application.usecase)
./gradlew koverHtmlReport
```

Los tests usan fakes en memoria; nunca llaman a Gemini, Stripe, Wompi ni remove.bg.
`PostgresIntegrationTest` (raíz) levanta Postgres con Testcontainers si hay Docker; si no, se salta.

## Variables de entorno

Ver [`.env.example`](.env.example) (todas comentadas). Mínimas para arrancar: `DB_*`, `JWT_SECRET`.
