# CloudFit API v1 — contrato back ↔ front

Fuente de verdad compartida entre `cloud-fit` (Ktor) y `cloudfit-frontend` (React).
Si algo cambia, se cambia aquí primero.

## Generalidades

- Base path: **`/api/v1`**. Front y API viven en el mismo origen:
  - local (Vite): proxy `/api` → `http://localhost:8080`
  - compose: nginx del `front` hace proxy `/api` → `back:8080`; el túnel manda todo al `front`.
- JSON en camelCase. Fechas ISO-8601 UTC (`2026-10-06T12:00:00Z`).
- Enums del dominio en MAYÚSCULAS (igual que hoy: `TOP`, `CASUAL`, …).
- `locale`: `"en" | "es"`.
- **Auth**: header `Authorization: Bearer <accessToken>` (JWT, 15 min).
  El refresh token va en cookie `cf_refresh` (`HttpOnly; SameSite=Lax; Path=/api/v1/auth; Secure` en prod, 30 días, rotativo).
  Las rutas `/auth/refresh` y `/auth/logout` exigen además el header `X-CloudFit-Client: web`.
- **Errores**: siempre `{ "error": "<CODE>", "message": "<texto dev, no mostrar>" }`. El front traduce por `error`.

| HTTP | error |
|---|---|
| 400 | `VALIDATION_ERROR` |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS`, `INVALID_TOKEN` |
| 402 | `INSUFFICIENT_CREDITS` |
| 403 | `EMAIL_NOT_VERIFIED`, `WARDROBE_LIMIT_REACHED`, `FORBIDDEN` |
| 404 | `NOT_FOUND` |
| 409 | `CONFLICT` |
| 422 | `WARDROBE_INCOMPLETE` |
| 429 | `RATE_LIMITED` |
| 501 | `PROVIDER_NOT_CONFIGURED` |
| 502 | `AI_UNAVAILABLE`, `UPSTREAM_ERROR` |

## Auth — `/auth` (públicas)

Respuesta de sesión (`Session`):
```json
{ "accessToken": "jwt", "expiresIn": 900, "user": Me }
```
+ `Set-Cookie: cf_refresh=...`

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| GET | `/auth/providers` | – | `{ "google": { "clientId": "…" } \| null, "apple": { "clientId": "…", "redirectUri": "…" } \| null }` |
| POST | `/auth/register` | `{ email, password, locale, displayName? }` | **202** `{ "status": "VERIFY_EMAIL" }` siempre (exista o no el email — no revela cuentas). Envía email de verificación (o "ya tienes cuenta"). |
| POST | `/auth/verify-email` | `{ token }` | 200 `Session`. Marca verificado y otorga **5 créditos** de bienvenida (idempotente). |
| POST | `/auth/resend-verification` | `{ email }` | 202 siempre |
| POST | `/auth/login` | `{ email, password }` | 200 `Session` · 401 `INVALID_CREDENTIALS` · 403 `EMAIL_NOT_VERIFIED` |
| POST | `/auth/google` | `{ idToken }` | 200 `Session` (crea cuenta si no existe, otorga bienvenida si es nueva) · 501 si no configurado |
| POST | `/auth/apple` | `{ idToken, displayName? }` | 200 `Session` · 501 si no configurado |
| POST | `/auth/refresh` | – (cookie) | 200 `Session` · 401 `INVALID_TOKEN` (rota cookie; reuso de un token viejo revoca toda la familia) |
| POST | `/auth/logout` | – (cookie) | 204, borra cookie |
| POST | `/auth/forgot-password` | `{ email }` | 202 siempre |
| POST | `/auth/reset-password` | `{ token, password }` | 204, revoca todas las sesiones |

- Password: 8–128 caracteres.
- Links de email: `${APP_URL}/verify-email?token=…` y `${APP_URL}/reset-password?token=…`.
- Rate limit por IP en todas las rutas de `/auth` → 429 `RATE_LIMITED`.
- Unión de cuentas: Google/Apple con email verificado por el proveedor se vincula a la cuenta existente con ese email.

## Me — `/me` (auth)

`Me`:
```json
{
  "id": "uuid",
  "email": "a@b.com",
  "emailVerified": true,
  "displayName": "Ana",
  "locale": "es",
  "avatarUrl": "https://…" ,
  "providers": ["PASSWORD", "GOOGLE"],
  "credits": { "balance": 5, "plan": 0, "free": 5, "pack": 0, "reward": 0 },
  "plan": { "code": "PLUS", "status": "ACTIVE", "renewsAt": "…" } ,
  "adsEnabled": true,
  "limits": { "maxClothes": 30 }
}
```
`avatarUrl`, `plan` pueden ser `null`. `adsEnabled = plan == null`. `limits.maxClothes` es `null` con plan activo.

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| GET | `/me` | – | `Me` |
| PATCH | `/me` | `{ displayName?, locale? }` | `Me` |
| DELETE | `/me` | – | 204 (borra cuenta y datos) |

## Armario — (auth)

`Clothe` (igual que hoy): `{ id, category, imageUrl, name?, colour?, pattern?, formality?, warmth?, description? }`
`category`: `TOP|BOTTOM|DRESS|SHOES|OUTERWEAR|ACCESSORIES` · `formality`: `CASUAL|SMART_CASUAL|FORMAL` · `warmth`: `LIGHT|MEDIUM|WARM`

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| GET | `/clothes` | – | `Clothe[]` del usuario |
| POST | `/clothes` | `Clothe` (id ignorado) | 201 `Clothe` · 403 `WARDROBE_LIMIT_REACHED` |
| PUT | `/clothes/{id}` | `Clothe` | `Clothe` · 404 |
| DELETE | `/clothes/{id}` | – | 204 · 404 |
| POST | `/uploads` | multipart `file` | 201 `{ url }` |
| POST | `/remove-bg` | multipart `file` | 200 `{ photoUrl }` · 501 si no hay key |
| POST | `/tagger` | multipart `file` | 200 `{ name, category, colour, pattern, formality, warmth, description }` · 502 |
| GET | `/avatar` | – | `{ photoUrl }` · 404 |
| PUT | `/avatar` | `{ photoUrl }` | `{ photoUrl }` |
| DELETE | `/avatar` | – | 204 |
| GET | `/default/clothes` | – | público |
| GET | `/default/avatars` | – | público |
| GET | `/images/{key}` | – | público, sirve imágenes cuando el store es local (dev) |

Subir, etiquetar y quitar fondo **no consume créditos**; el tope de prendas lo da `limits.maxClothes`.

## Looks — generación asíncrona (auth)

`Look`:
```json
{
  "id": "uuid",
  "event": "boda en la playa",
  "status": "QUEUED|PICKING|RENDERING|READY|FAILED",
  "clotheIds": ["…"],
  "stylistNote": "…",
  "imageUrl": "https://…",
  "saved": false,
  "failureCode": "AI_UNAVAILABLE",
  "createdAt": "…"
}
```
`clotheIds`/`stylistNote` llegan cuando termina `PICKING`; `imageUrl` cuando `READY`.

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| POST | `/looks` | `{ event }` | **202** `Look` (status `QUEUED`). Reserva 1 crédito. 402 `INSUFFICIENT_CREDITS` · 422 `WARDROBE_INCOMPLETE` · 429 |
| GET | `/looks/{id}` | – | `Look` (el front hace polling cada ~2 s hasta `READY`/`FAILED`) |
| GET | `/looks?saved=true` | – | `Look[]` más recientes primero |
| POST | `/looks/{id}/save` | – | `Look` (`saved: true`) |
| DELETE | `/looks/{id}` | – | 204 |

- Créditos: reserva al crear → confirma en `READY` → **reembolso automático** en `FAILED`.
- La nota del estilista se genera en el `locale` del usuario.
- `WARDROBE_INCOMPLETE`: falta (top+bottom o dress) + shoes.

## Billing — (auth salvo webhooks)

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| GET | `/billing/catalog` | `?country=CO` opcional (por defecto header `CF-IPCountry`) | `Catalog` |
| GET | `/billing/ledger` | `?limit=50` | `LedgerEntry[]` |
| POST | `/billing/checkout` | `{ productCode, provider? }` | `{ checkoutUrl }` (redirigir) · 501 si el provider no está configurado |
| POST | `/billing/portal` | – | `{ url }` portal de Stripe para gestionar/cancelar plan |
| POST | `/billing/webhooks/stripe` | raw (firma `Stripe-Signature`) | 200 |
| POST | `/billing/webhooks/wompi` | JSON (checksum Wompi) | 200 |
| POST | `/ads/reward` | `{ placement }` | `{ granted: true, balance }` · 429 al superar 3/día · 403 si tiene plan |

`Catalog`:
```json
{
  "provider": "STRIPE|WOMPI|FAKE",
  "currency": "USD|COP",
  "packs": [{ "code": "PACK_10", "credits": 10, "price": 499, "currency": "USD" }],
  "plans": [{ "code": "PLUS", "monthlyCredits": 40, "price": 799, "currency": "USD", "features": ["NO_ADS", "UNLIMITED_CLOTHES"] }]
}
```
- `price` en unidades menores (centavos USD / pesos COP sin decimales ×100).
- Colombia (`CO`) → `WOMPI`, solo packs (`plans: []`). Resto → `STRIPE`, packs + planes.
- Dev: `PAYMENTS_MODE=fake` → provider `FAKE`, `checkout` acredita al instante y devuelve `checkoutUrl = "/billing/success?fake=1"`.
- Retornos del checkout: `${APP_URL}/billing/success` y `${APP_URL}/billing/cancel`.

`LedgerEntry`: `{ id, delta, bucket: "FREE|PACK|PLAN|REWARD", reason: "SIGNUP_BONUS|PURCHASE|PLAN_GRANT|PLAN_EXPIRE|LOOK_RESERVED|LOOK_REFUND|AD_REWARD", createdAt }`

Orden de consumo de créditos: `PLAN` → `FREE` → `REWARD` → `PACK`.

## Health

`GET /health` (fuera de `/api/v1` y también en `/api/v1/health`) → `{ "status": "ok" }`.
