# RYVENCA Backend

> *Style what you already own.*

REST API for RYVENCA, the digital wardrobe and outfit suggestion app. Users upload ordinary phone
photos of their own clothes. The API detects each garment's color, organizes the wardrobe, and builds
scored, explained outfits using only pieces the user already owns.

- **Stack:** Java 21 · Spring Boot 4.1 · Spring Security (JWT resource server) · Spring Data JPA /
  Hibernate 7 · Flyway · PostgreSQL (H2 for local development and tests)
- **API contract:** [`docs/API.md`](docs/API.md). The web (`ryvenca_web`) and mobile (`ryvenca_mobile`)
  apps are built against it.

## Quick start

```bash
./mvnw spring-boot:run          # http://localhost:8080, H2 file DB in ./data, photos in ./data/media
./mvnw test                     # unit + integration tests
```

The default profile needs no setup (H2 in PostgreSQL mode). For a production-like stack:

```bash
RYVENCA_JWT_SECRET=$(openssl rand -base64 48) docker compose up --build
```

### Configuration

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | – | `prod` requires the DB and secret variables below |
| `RYVENCA_DB_URL` / `_USER` / `_PASSWORD` | H2 file DB | JDBC connection, e.g. `jdbc:postgresql://db:5432/ryvenca` |
| `RYVENCA_JWT_SECRET` | dev-only value | HS256 signing secret, **≥ 32 bytes**, mandatory in `prod` |
| `RYVENCA_TOKEN_TTL` | `30d` | Access token lifetime |
| `RYVENCA_CORS_ORIGINS` | `http://localhost:5173,…` | Comma separated allowed web origins |
| `RYVENCA_STORAGE_DIR` | `./data/media` | Where photos are stored |
| `RYVENCA_PUBLIC_BASE_URL` | derived from request | Absolute base for media URLs behind a proxy/CDN |
| `PORT` | `8080` | HTTP port |

## Sign-in (Firebase: Apple, Google, e-mail)

Clients sign in with the Firebase SDK and exchange the Firebase ID token at `POST /api/auth/firebase` for
a RYVENCA JWT. The server verifies the token with the Firebase Admin SDK, finds the account by Firebase
UID, links it by verified e-mail, or creates it. The only thing the server needs is the service account file:

```
config/firebase-service-account.json      # or RYVENCA_FIREBASE_CREDENTIALS=/path/file.json
```

See [`config/README.md`](config/README.md). Without it, `/api/config` reports `auth.firebase: false` and
only local e-mail/password accounts work. You can switch those off with `RYVENCA_LOCAL_AUTH=false` once
Firebase is live.

| Variable | Default | Description |
|---|---|---|
| `RYVENCA_FIREBASE_CREDENTIALS` | `./config/firebase-service-account.json` | Firebase service account JSON |
| `RYVENCA_FIREBASE_PROJECT_ID` | from the file | Override the Firebase project id |
| `RYVENCA_LOCAL_AUTH` | `true` | Legacy local e-mail/password accounts (development) |
| `RYVENCA_ADMIN_EMAILS` | – | Comma-separated e-mails that become admins when they sign in |

## Admin panel API

Admins (`role: ADMIN`) manage the following through `/api/admin/**`; see `docs/API.md`:

- **Runtime settings:** sign-in providers, Firebase web config, privacy/terms/support/deletion/store links,
  maintenance mode and minimum app versions. Clients read them publicly from `GET /api/config`.
- **Users:** search, promote/demote, disable, delete.
- **Deletion requests** and the **anonymized deletion log**.
- **Color palettes:** the outfit engine's dataset. The 45 built-in palettes are seeded into the `palettes`
  table on startup. Admins can rename (per language), recolor or disable them, and add their own.
  Changes apply immediately.

The first admin comes from `RYVENCA_ADMIN_EMAILS`. That admin can then promote others in the panel.

## Account deletion (App Store 5.1.1(v) / Google Play)

- **In the app or on the website:** `DELETE /api/me` removes the account immediately:
  - photos, garments and saved outfits;
  - the account row;
  - the Firebase Authentication user.

  Apple sign-in tokens are revoked by the client before the call.
- **Without access to the account:** the public deletion page posts
  `POST /api/account-deletion-requests` (rate limited, no account enumeration). An admin approves or
  rejects the request in the panel.
- Each completed deletion leaves only an anonymized entry: salted e-mail hash, provider, method
  (`IN_APP`, `WEB`, `REQUEST`, `ADMIN`), optional reason, and date.

## Languages (i18n)

The API speaks 16 languages: Turkish (product default) plus the 15 most spoken languages: `tr en zh hi es
ar fr bn pt ru id ur de ja vi ko` (Arabic and Urdu are RTL on the clients).

- The request language comes from `Accept-Language` (`LanguageLocaleResolver`). Matching is on the primary
  subtag. No header means Turkish; an unsupported language means English.
- Every server-rendered text comes from `src/main/resources/i18n/messages_<code>.properties`
  (`messages.properties` = English, also the fallback). This covers enum labels in `/api/meta`, generated
  garment names, outfit titles, "why it works" explanations, venues, palette names, readiness hints and
  error/validation messages. Messages are read raw, so apostrophes need no escaping, and `{0}`, `{1}`
  placeholders are filled by `Localizer`.
- Explanations are complete sentence templates (`explain.*`, `title.*`). Per-language grammar lives in
  the same files:
  - `grammar.phrase`: how a color and a garment form a phrase, e.g. "beige blazer", "Blazer in Beige",
    "ベージュのブレザー".
  - `grammar.list.*`: how lists are joined.
  - `grammar.sentences`: how sentences are joined.
  - `grammar.lowercase`: whether labels are lowercased mid-sentence.
  - `garment.generatedName`: the auto name of a garment.
- `User.language` stores the preferred UI language (`PUT /api/me {"language": "ja"}`).
- `MessageFilesTest` fails if any language file misses a key or changes a key's placeholders.

To add a language, add it to `Language`, create `messages_<code>.properties` with the same keys, and run the
tests.

## Architecture

```
com.ryvenca
├── auth        Firebase sign-in exchange, local register/login, JWT issuing (Nimbus, HS256)
├── firebase    Firebase Admin SDK gateway (token verification, user deletion)
├── settings    admin-managed runtime settings, public /api/config
├── admin       admin panel API (stats, users, settings, deletion requests, palettes)
├── deletion    account deletion, deletion requests, anonymized deletion log
├── palette     database-backed palette dataset (seeded, admin-editable)
├── user        profile, style preferences, onboarding, account deletion
├── i18n        supported languages, Accept-Language resolution, Localizer (templates + grammar)
├── catalog     domain enums: Category, Subcategory (formality, fabric feel, warmth, default
│               seasons/occasions, style affinities), Season, Occasion, StylePreference, /api/meta
├── color       CIELAB + CIEDE2000, 17 standard color classes, dominant color detection,
│               curated palette dataset (resources/palettes.json)
├── image       upload pipeline (EXIF orientation, resize, gentle normalization, 4:5 thumbnail),
│               local media storage served under /media/**, draft cleanup
├── garment     digital wardrobe CRUD, filters, favorites
├── outfit      outfit engine, saved outfits, suggestions, pairings, similar outfits
└── home        home screen aggregate
```

### Color detection (no generative AI)

`DominantColorDetector` works on ordinary photos: garment on a bed, a hanger, a sofa, or the floor.

1. The photo is downscaled and converted to CIELAB.
2. The **background** is estimated with k-means on the border strip. A border color that also fills
   the image center is treated as the garment (close-up photos).
3. Pixels in the central region of interest that differ from the background are weighted towards
   the center and clustered with k-means++. Clusters closer than ΔE 10 are merged, so shadows and
   highlights of one fabric count as one color.
4. The dominant cluster is mapped with **CIEDE2000** to the nearest of the 17 classes. Each class has
   several real-world prototypes (for example raw vs. washed denim for *Mavi*, camel for *Bej*).
   Confidence and the top-3 candidates are returned. Large, clearly different secondary clusters set
   `patternLikely`.

The measured tone (`colorHex`) is kept for scoring. The user can always override the class (manual
override → `colorSource: MANUAL`).

### Outfit engine

Every outfit is TOP + BOTTOM (or DRESS) + SHOES, optionally with OUTERWEAR, BAG and ACCESSORY. The
**Uyum Skoru** combines five dimensions:

| Dimension | Weight | How |
|---|---|---|
| Color | 40% | 50% closeness to a curated palette (CIEDE2000, near colors accepted, neutrals get an allowance) + 50% color-theory rules: number of statement colors, analogous/complementary hues, tonal (*ton sür ton*) and monochrome dressing, near-clashes (e.g. navy vs. black), pattern mixing, bag/shoe echo |
| Garment | 25% | Formality coherence between pieces, fabric variety, known clashes (sweatpants + loafers) and good pairings (blazer + jeans, shirt + trousers, knit + tailored) |
| Season | 15% | Pieces' seasons and outfit warmth (a puffer in summer, sandals in winter) |
| Occasion | 15% | Occasion tags plus overall formality vs. the occasion's band |
| Style | 5% | Style expressed by the outfit vs. the user's style preferences |

The weighted mean is stretched with a power curve so that clashes land clearly lower (~55–65) than
good outfits (~85–96). `OutfitExplainer` turns the analyses into Turkish copy: title, description,
and the **Neden Uyumlu?** cards (Renk Dengesi, Doku Uyumu, Kullanım Alanı, optionally Mevsim), plus
the venues (*Ofis, Toplantı, Akşam Yemeği…*).

Candidate search: season/occasion-aware pools → best top×bottom pairs (beam) + dresses → × shoes →
enrichment with layer variants, bag and accessory → seeded jitter (daily seed for *Bugünün Önerileri*,
random seed for *Yeniden öner*) → diversity-aware greedy selection. The same engine answers
**Bununla ne gider?** (outfits anchored on one piece + best matches per role) and **Benzer Kombinler**.

### Photos

Photos are never turned into catalog images and there is no background removal. The pipeline only
fixes EXIF orientation, caps the size (original 2400 px, display 1440 px), applies a light
percentile-based brightness/contrast normalization, and creates a 4:5 thumbnail centered on the
detected garment. Uploaded photos stay drafts until a garment references them. Drafts are purged
after 24 hours. Media file names are random UUIDs served with long-lived cache headers.

## Tests

- `ColorScienceTest`: CIEDE2000 against the Sharma et al. reference data
- `ColorClassifierTest`, `DominantColorDetectorTest`: real-world tones and synthetic "phone photos"
  (textured backgrounds, uneven light, noise, stripes)
- `ImageProcessorTest`: all 8 EXIF orientations, thumbnails, normalization
- `OutfitEngineTest`, `OutfitExplainerTest`: score ordering, season/occasion behavior, diversity,
  pairings, similar outfits, Turkish explanations
- `EngineBench` (opt-in, `./mvnw test -Dtest=EngineBench -Dbench=true`): timings on a 200-piece
  wardrobe (≈ 250 ms for suggestions once warm)
- `AccountAndAdminIntegrationTest`: Firebase sign-in with a fake verifier (creation, linking, disabled
  accounts), admin settings/users/palettes, in-app deletion, reviewed deletion requests, rate limiting
- `ApiFlowIntegrationTest`: end-to-end over MockMvc (auth, upload, detection, manual override,
  suggestions, save, pairings, home, isolation between users, account deletion)
