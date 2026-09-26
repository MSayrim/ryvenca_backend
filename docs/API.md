# RYVENCA API (v1)

Base path: `/api`. JSON everywhere except image upload (multipart).
All endpoints except `/api/auth/**`, `/api/meta`, `/api/config`, `POST /api/account-deletion-requests` and `/media/**` require
`Authorization: Bearer <token>`.

Error body (any 4xx/5xx):

```json
{ "status": 400, "error": "VALIDATION_ERROR", "message": "Türkçe açıklama", "fieldErrors": { "email": "Geçerli bir e-posta gir" } }
```

Error codes: `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`,
`INVALID_IMAGE`, `PAYLOAD_TOO_LARGE`, `ACCOUNT_DISABLED`, `LOCAL_AUTH_DISABLED`, `AUTH_UNAVAILABLE`, `INTERNAL_ERROR`. `message` is always user-presentable, in the request language (see Languages).

## Languages (i18n)

Supported language codes (16): `tr` (Türkçe, product default), `en`, `zh` (Simplified Chinese), `hi`, `es`,
`ar` (RTL), `fr`, `bn`, `pt`, `ru`, `id`, `ur` (RTL), `de`, `ja`, `vi`, `ko`.

- Clients send `Accept-Language: <code>` (the UI language the user picked) on **every** request.
  The server matches on the language subtag (`zh-CN` → `zh`, `pt-BR` → `pt`). No header → `tr`;
  unsupported language → `en`.
- Everything the server renders is localized for that language: all `label`/`pluralLabel`/`description`
  fields in `/api/meta`, `Garment.displayName` (generated names such as "Beige blazer"), outfit `title`,
  `description`, `reasons`, `venues`, `styleLabel`, `paletteName`, `breakdown[].label`, readiness messages,
  pairing `label`s and every error `message` / `fieldErrors` value.
  Clients must therefore include the language in the cache key of every server-rendered query and refetch
  (or invalidate) when the language changes.
- A user-chosen title of a saved outfit is returned as entered.
- `GET /api/meta` also returns `"languages": [{ "code": "ar", "label": "العربية", "rtl": true }]` (labels are
  native names, the same in every UI language).
- `User.language` (`string | null`) stores the preferred UI language so it follows the user across devices;
  set it with `PUT /api/me { "language": "ja" }`. Clients apply it after login when present; otherwise they
  use the device/browser language when supported, else `en`.

## Enums

| Enum | Values |
|---|---|
| WardrobeType | `WOMEN`, `MEN`, `UNISEX` |
| StylePreference | `CASUAL`, `SMART_CASUAL`, `MINIMAL`, `CLASSIC`, `STREETWEAR`, `BUSINESS`, `SPORT` |
| Category | `TOP`, `BOTTOM`, `OUTERWEAR`, `DRESS`, `SHOES`, `BAG`, `ACCESSORY` |
| Subcategory | see `/api/meta` (e.g. `T_SHIRT`, `SHIRT`, `JEANS`, `BLAZER`, `SNEAKER`, `LOAFER` …) |
| ColorName | `BLACK`, `WHITE`, `GRAY`, `CREAM`, `BEIGE`, `BROWN`, `NAVY`, `BLUE`, `LIGHT_BLUE`, `GREEN`, `OLIVE`, `RED`, `BURGUNDY`, `ORANGE`, `YELLOW`, `PINK`, `PURPLE` |
| Season | `SPRING`, `SUMMER`, `AUTUMN`, `WINTER` |
| Occasion | `DAILY`, `OFFICE`, `EVENING`, `WEEKEND`, `SPORT` |
| OutfitRole | `OUTERWEAR`, `TOP`, `DRESS`, `BOTTOM`, `SHOES`, `BAG`, `ACCESSORY` |

Localized labels for every enum value come from `GET /api/meta`; clients must not hard-code labels.

## Meta

`GET /api/meta` (public, cacheable forever per app session)

```json
{
  "languages": [{ "code": "tr", "label": "Türkçe", "rtl": false }],
  "wardrobeTypes": [{ "code": "WOMEN", "label": "Kadın" }],
  "styles": [{ "code": "SMART_CASUAL", "label": "Smart Casual", "description": "Rahat ama özenli" }],
  "categories": [
    { "code": "TOP", "label": "Üst", "pluralLabel": "Üstler",
      "subcategories": [{ "code": "SHIRT", "label": "Gömlek" }] }
  ],
  "colors": [{ "code": "BEIGE", "label": "Bej", "hex": "#CDB89A" }],
  "seasons": [{ "code": "SPRING", "label": "İlkbahar" }],
  "occasions": [{ "code": "OFFICE", "label": "Ofis" }]
}
```

Array order is the intended display order.

## Auth

`POST /api/auth/register` `{ "email", "password" (min 8), "displayName" }` → `201 AuthResponse`
`POST /api/auth/login` `{ "email", "password" }` → `200 AuthResponse` (401 on bad credentials)

```json
AuthResponse = { "token": "jwt", "expiresAt": "2026-10-25T10:00:00Z", "user": User }
User = {
  "id": 1, "email": "a@b.com", "displayName": "Ayşe",
  "wardrobeType": "WOMEN" | null,
  "language": "tr" | null,
  "stylePreferences": ["MINIMAL", "CLASSIC"],
  "onboardingCompleted": false,
  "createdAt": "..."
}
```

`GET /api/me` → `User`
`PUT /api/me` `{ "displayName"?, "wardrobeType"?, "stylePreferences"?, "onboardingCompleted"?, "language"? }` → `User` (only non-null fields are applied)
`DELETE /api/me` → `204` (deletes account, garments, images, saved outfits — see Account deletion)

## Sign-in with Firebase (Apple, Google, e-mail)

Clients sign in with the Firebase SDK (Sign in with Apple, Google, e-mail + password), then exchange the
Firebase ID token for a RYVENCA token. Every other endpoint keeps using the RYVENCA JWT as before.

`POST /api/auth/firebase` `{ "idToken": "<Firebase ID token>", "displayName": "Ayşe" | null }` → `200 AuthResponse`

- The server verifies the token with the Firebase Admin SDK, then finds the user by Firebase UID, else by
  (verified) e-mail (accounts are linked), else creates one. `displayName` is only used when creating a
  user and the token carries no name (Apple returns the name to the client only on the first sign-in).
- `401 UNAUTHORIZED` invalid/expired token · `403 ACCOUNT_DISABLED` · `503 AUTH_UNAVAILABLE` when the
  server has no Firebase credentials configured.
- E-mail verification mails and password reset mails are sent by Firebase (client SDK).

Local e-mail/password (`/api/auth/register`, `/api/auth/login`) remains for development and for setups
without Firebase; it is only available while `config.auth.local` is `true` (else `403 LOCAL_AUTH_DISABLED`).

`User` additionally contains:

```json
{ "role": "USER" | "ADMIN", "authProvider": "APPLE" | "GOOGLE" | "PASSWORD" | "LOCAL", "emailVerified": true }
```

Any authenticated request of a disabled account → `403 ACCOUNT_DISABLED` (clients sign the user out and
show the message).

## Public app configuration

`GET /api/config` (public, no auth; values are managed in the admin panel)

```json
{
  "auth": { "firebase": true, "local": false,
            "providers": { "apple": true, "google": true, "email": true } },
  "firebaseWeb": { "apiKey": "…", "authDomain": "…", "projectId": "…", "appId": "…",
                   "messagingSenderId": "…", "storageBucket": "…" } | null,
  "links": { "privacyPolicy": "https://…" | null, "terms": "https://…" | null, "support": "https://…" | null,
             "supportEmail": "help@…" | null, "accountDeletion": "https://…" | null,
             "appStore": "https://…" | null, "playStore": "https://…" | null },
  "maintenance": { "enabled": false, "message": "…" | null },
  "minVersion": { "ios": "1.0.0" | null, "android": "1.0.0" | null }
}
```

- `auth.firebase`: the server can verify Firebase tokens. Show only the providers that are `true`
  (and Apple only where supported). `auth.local`: the legacy e-mail/password form may be shown.
- `firebaseWeb`: the web app initializes Firebase with this at runtime (so the web keys can be entered in
  the admin panel); `null` = not configured (web falls back to `VITE_FIREBASE_*` env, then to local auth).
- Clients show privacy policy / terms / support links where present (required for store review).
- `maintenance.enabled` → clients show a blocking maintenance screen with `message`.
- `minVersion.<platform>` → mobile shows a blocking "update the app" screen with the store link when the
  installed version is lower.

## Account deletion

1. **In the app / signed in on the web** — `DELETE /api/me` `{ "reason": "…" | null }` (body optional) → `204`.
   The mobile app sends `X-Client: mobile/<app version>` (on every request) so the deletion is logged as
   `IN_APP`; without the header it is logged as `WEB`.
   Deletes the account, garments, photos, saved outfits and the Firebase Authentication user, immediately.
   Before calling it, clients that used Sign in with Apple revoke the Apple token (Firebase
   `revokeAccessToken` / RN Firebase `auth().revokeToken(authorizationCode)` after an Apple re-auth), and
   Google users are signed out/revoked from Google Sign-In.
2. **Without access to the account** (lost login; linked from the public deletion page, required by Google
   Play) — `POST /api/account-deletion-requests` (public) `{ "email": "…", "message": "…" | null }` →
   `202 { "reference": "DR-7K2Q9M" }`. Always 202 (no account enumeration); a pending request for the same
   e-mail returns the same reference. Requests are reviewed and approved in the admin panel.

Every completed deletion is written to an anonymized deletion log (hashed e-mail, provider, method, date).

## Admin API (role `ADMIN`)

All under `/api/admin/**`, `403 FORBIDDEN` for non-admins. Admins are bootstrapped from the
`RYVENCA_ADMIN_EMAILS` environment variable (comma separated; applied at sign-in) and can promote others.

- `GET /api/admin/stats` → `{ "users": 120, "newUsers7d": 14, "garments": 1830, "savedOutfits": 412,
  "pendingDeletionRequests": 2, "deletionsLast30d": 3 }`
- `GET /api/admin/settings` → `AdminSettings`; `PUT /api/admin/settings` (partial, only non-null fields) → `AdminSettings`

  ```json
  AdminSettings = {
    "providers": { "apple": true, "google": true, "email": true },
    "firebaseWeb": { "apiKey": "", "authDomain": "", "projectId": "", "appId": "", "messagingSenderId": "", "storageBucket": "" },
    "links": { …same keys as config.links… },
    "maintenance": { "enabled": false, "message": null },
    "minVersion": { "ios": null, "android": null },
    "status": { "firebaseAdmin": true, "firebaseProjectId": "ryvenca-prod" | null, "localAuth": false }
  }
  ```
  `status` is read-only (what the server itself has configured). URLs must be http(s), `supportEmail` an
  e-mail, versions `major.minor.patch`; empty string clears a value.
- `GET /api/admin/users?q=&page=0&size=20` → `{ "items": [AdminUser], "total": 120, "page": 0, "size": 20 }`
  `AdminUser = { "id", "email", "displayName", "role", "authProvider", "disabled", "language",
  "garmentCount", "savedOutfitCount", "createdAt" }` (newest first; `q` matches e-mail or name)
- `PATCH /api/admin/users/{id}` `{ "role"?: "ADMIN"|"USER", "disabled"?: true }` → `AdminUser`
  (admins cannot demote/disable themselves)
- `DELETE /api/admin/users/{id}` → `204` (full deletion as above, logged with method `ADMIN`)
- `GET /api/admin/deletion-requests?status=PENDING|COMPLETED|REJECTED` →
  `[{ "id", "reference", "email", "message", "language", "status", "matchedUserId": 12 | null,
     "createdAt", "resolvedAt", "resolvedBy", "note" }]` (newest first; `matchedUserId` = account with that e-mail)
- `POST /api/admin/deletion-requests/{id}/approve` `{ "note"?: "…" }` → request (deletes the matched account
  if any; status `COMPLETED`)
- `POST /api/admin/deletion-requests/{id}/reject` `{ "note": "…" }` → request (status `REJECTED`)
- `GET /api/admin/deletion-log?limit=100` → `[{ "method": "IN_APP"|"WEB"|"REQUEST"|"ADMIN", "authProvider",
  "reason", "createdAt" }]`
- Palettes (the curated color combinations the outfit engine uses; built-in ones can be edited/disabled,
  custom ones added/deleted):
  - `GET /api/admin/palettes` → `[{ "id": "P001", "colors": ["#EFE6D2", …], "names": { "en": "Earth Tones", "tr": "Toprak Tonları" }, "enabled": true, "builtIn": true }]`
    (`names` holds admin-entered names; built-in palettes fall back to the translated names in the server's
    message files, which are included here for all 16 languages)
  - `POST /api/admin/palettes` `{ "colors": [2–5 hex], "names": { "en": "…", …any languages }, "enabled": true }` → palette (`names.en` required)
  - `PUT /api/admin/palettes/{id}` same body → palette · `DELETE /api/admin/palettes/{id}` → `204` (custom only)

## Images & color detection

`POST /api/images` multipart field `file` (JPEG / PNG / WebP, max 15 MB) → `201 ImageUpload`.
Stores the photo (auto-rotated via EXIF, lightly normalized, 4:5 thumbnail) and detects the dominant garment color.
The image is a draft until a garment references it; unreferenced drafts are purged after 24h.

```json
ImageUpload = {
  "imageId": "7f3c…uuid",
  "imageUrl": "https://host/media/7f3c…-display.jpg",
  "thumbnailUrl": "https://host/media/7f3c…-thumb.jpg",
  "width": 1080, "height": 1440,
  "detection": {
    "color": "BEIGE", "hex": "#C8B596", "confidence": 0.82,
    "patternLikely": false,
    "candidates": [ { "color": "BEIGE", "hex": "#CDB89A", "score": 0.82 }, { "color": "CREAM", "hex": "#EFE6D2", "score": 0.55 } ]
  }
}
```

`hex` in `detection` is the measured garment color; candidate `hex` values are the canonical swatches.
`patternLikely` is a hint that the garment is striped/patterned (clients may pre-toggle "Desenli").

## Garments

```json
Garment = {
  "id": 12, "name": "Bej blazer" | null,
  "displayName": "Bej Blazer",           // name, or generated "<Renk> <Alt kategori>"
  "category": "OUTERWEAR", "subcategory": "BLAZER",
  "color": "BEIGE", "colorHex": "#C8B596", "colorSource": "AUTO" | "MANUAL",
  "pattern": false,
  "seasons": ["SPRING", "AUTUMN"], "occasions": ["OFFICE", "EVENING"],
  "favorite": false,
  "imageId": "uuid", "imageUrl": "…", "thumbnailUrl": "…",
  "createdAt": "…", "updatedAt": "…"
}
```

`POST /api/garments` → `201 Garment`

```json
{ "imageId": "uuid", "name": null, "category": "OUTERWEAR", "subcategory": "BLAZER",
  "color": "BEIGE", "colorHex": null, "pattern": false,
  "seasons": ["SPRING","AUTUMN"], "occasions": ["OFFICE"] }
```

- `subcategory` must belong to `category` (validated).
- `seasons` / `occasions` optional: empty or missing → sensible defaults derived from the subcategory.
- `colorHex` optional: when `color` equals the detected color the measured hex is kept; if the user picked
  a different color (manual override) the canonical hex is used and `colorSource` becomes `MANUAL`.

`GET /api/garments?category=TOP&color=BEIGE&season=WINTER&occasion=OFFICE&favorite=true&q=blazer`
→ `Garment[]` newest first. All filters optional; `category`, `color`, `season`, `occasion` accept comma separated lists.

`GET /api/garments/{id}` → `Garment`
`PUT /api/garments/{id}` same body as create (`imageId` optional, replaces the photo) → `Garment`
`DELETE /api/garments/{id}` → `204` (saved outfits containing it are deleted too)
`PUT /api/garments/{id}/favorite` `{ "favorite": true }` → `Garment`

`GET /api/garments/{id}/pairings?season=&occasion=` — "Bununla ne gider?"

```json
{
  "anchor": Garment,
  "matches": [ { "role": "TOP", "label": "Üst", "items": [ { "garment": Garment, "score": 88 } ] } ],
  "outfits": [ Outfit ],
  "readiness": Readiness
}
```

`matches` lists, per complementary role, up to 6 best items ranked by how well they pair with the anchor.

## Outfits

```json
Outfit = {
  "key": "3-12-40-51",                      // sorted garment ids, stable identity of an outfit
  "title": "Zamansız Şıklık",
  "description": "Klasik parçalarla modern ve sade bir görünüm.",
  "score": 94,
  "breakdown": [ { "code": "COLOR", "label": "Renk uyumu", "weight": 40, "score": 92 },
                 { "code": "CATEGORY", "label": "Parça uyumu", "weight": 25, "score": 95 },
                 { "code": "SEASON", "label": "Mevsim", "weight": 15, "score": 100 },
                 { "code": "OCCASION", "label": "Kullanım alanı", "weight": 15, "score": 90 },
                 { "code": "STYLE", "label": "Stil", "weight": 5, "score": 80 } ],
  "style": "CLASSIC", "styleLabel": "Classic",
  "primaryOccasion": "OFFICE",
  "occasions": ["OFFICE", "EVENING"],
  "venues": ["Ofis", "Toplantı", "Akşam Yemeği", "Kahve Buluşması"],
  "items": [ { "role": "OUTERWEAR", "garment": Garment } ],   // display order: OUTERWEAR, TOP, DRESS, BOTTOM, SHOES, BAG, ACCESSORY
  "palette": [ { "color": "BEIGE", "label": "Bej", "hex": "#C8B596" } ],
  "paletteName": "Toprak Tonları" | null,
  "reasons": [ { "code": "COLOR", "title": "Renk Dengesi", "text": "…" },
               { "code": "TEXTURE", "title": "Doku Uyumu", "text": "…" },
               { "code": "OCCASION", "title": "Kullanım Alanı", "text": "…" } ],
  "saved": false, "savedId": null
}

Readiness = { "ready": true, "garmentCount": 12, "recommendedMinimum": 8,
              "missing": [ { "category": "SHOES", "message": "Kombin önerebilmemiz için en az bir ayakkabı ekle." } ] }
```

`GET /api/outfits/suggestions?occasion=OFFICE&season=AUTUMN&limit=10&seed=123`
→ `{ "outfits": Outfit[], "readiness": Readiness, "season": "AUTUMN", "occasion": "OFFICE" | null }`

- `occasion` optional (omit for "Tümü"). `season` defaults to the current season.
- `seed` optional. Omitted → stable per day ("Bugünün Önerileri"). Send a random value for "Yeni öneriler".
- Every outfit contains TOP+BOTTOM (or DRESS) + SHOES; OUTERWEAR, BAG, ACCESSORY are optional.

`GET /api/outfits/evaluate?items=3,12,40` → `Outfit` (400 if the combination is not a valid outfit)
`GET /api/outfits/similar?items=3,12,40&limit=6` → `{ "outfits": Outfit[] }`

`GET /api/outfits/saved` → `Outfit[]` (each with `saved: true`, `savedId`) newest first
`POST /api/outfits/saved` `{ "garmentIds": [3,12,40], "title": null }` → `201 Outfit` (idempotent: same key returns the existing one)
`DELETE /api/outfits/saved/{savedId}` → `204`

## Home

`GET /api/home`

```json
{
  "stats": { "garmentCount": 128, "readyOutfitCount": 24, "favoriteCount": 15 },
  "recentGarments": Garment[],        // up to 12, newest first
  "todaysSuggestions": Outfit[],      // up to 4, same as suggestions without seed
  "readiness": Readiness,
  "season": "AUTUMN"
}
```

`favoriteCount` = favorite garments + saved outfits. `readyOutfitCount` = number of distinct outfits scoring ≥ 70 (capped at 99).

## Media

`GET /media/{file}` public, long cache. URLs in responses are absolute and already point here.
