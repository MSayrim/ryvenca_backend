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

## Architecture

```
com.ryvenca
├── auth        register/login, JWT issuing (Nimbus, HS256)
├── user        profile, style preferences, onboarding, account deletion
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
- `ApiFlowIntegrationTest`: end-to-end over MockMvc (auth, upload, detection, manual override,
  suggestions, save, pairings, home, isolation between users, account deletion)
