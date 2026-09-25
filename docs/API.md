# RYVENCA API (v1)

Base path: `/api`. JSON everywhere except image upload (multipart).
All endpoints except `/api/auth/**`, `/api/meta` and `/media/**` require
`Authorization: Bearer <token>`.

Error body (any 4xx/5xx):

```json
{ "status": 400, "error": "VALIDATION_ERROR", "message": "Türkçe açıklama", "fieldErrors": { "email": "Geçerli bir e-posta gir" } }
```

Error codes: `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`,
`INVALID_IMAGE`, `PAYLOAD_TOO_LARGE`, `INTERNAL_ERROR`. `message` is always user-presentable, in the request language (see Languages).

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
`DELETE /api/me` → `204` (deletes account, garments, images, saved outfits)

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
