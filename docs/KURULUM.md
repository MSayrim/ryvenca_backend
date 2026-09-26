# RYVENCA — Kurulum kontrol listesi (ürün sahibi)

Kod, yapılandırma ve admin paneli hazır. Senin yapman gereken: aşağıdaki **dosyaları koymak**, **URL'leri/ayarları
girmek** ve konsollarda birkaç düğmeye basmak. Kod değiştirmen gerekmiyor.

## Özet: nereye ne konacak

| Ne | Nereden | Nereye |
|---|---|---|
| Service account JSON | Firebase → ⚙ Project settings → *Service accounts* → **Generate new private key** | Backend: `ryvenca_backend/config/firebase-service-account.json` (veya `RYVENCA_FIREBASE_CREDENTIALS=/yol/dosya.json`) |
| `google-services.json` | Firebase → Project settings → *Your apps* → Android `com.ryvenca.app` | Mobil: `ryvenca_mobile/firebase/google-services.json` |
| `GoogleService-Info.plist` | Firebase → Project settings → *Your apps* → iOS `com.ryvenca.app` | Mobil: `ryvenca_mobile/firebase/GoogleService-Info.plist` |
| Web `firebaseConfig` kodu | Firebase → Project settings → *Your apps* → Web app → *Config* | Web admin paneli: `/admin` → **Ayarlar** → *Firebase web yapılandırması* → "Config kodunu yapıştır" → "Alanlara aktar" → **Kaydet** |
| Apple `.p8` anahtarı + Key ID + Team ID | Apple Developer → *Keys* | Firebase → Authentication → Sign-in method → **Apple** → *OAuth code flow configuration* |

Bu dosyaların hepsi git dışında tutulur (`.gitignore`); repoya girmezler.

## 1. Firebase projesi

1. [console.firebase.google.com](https://console.firebase.google.com) → **Add project** (ör. `ryvenca-prod`).
2. **Uygulamaları ekle** (Project settings → *Your apps* → *Add app*):
   - **Web** (`</>`): adı serbest.
   - **Android**: paket adı **`com.ryvenca.app`**. Ardından *Add fingerprint* ile **SHA-1 ve SHA-256** ekle:
     - EAS anahtarı: `eas credentials --platform android` (veya expo.dev → Credentials).
     - Google Play App Signing anahtarı: Play Console → *Test ve yayınla → Kurulum → Uygulama imzalama*.
   - **iOS**: bundle ID **`com.ryvenca.app`**.
3. **Authentication → Sign-in method**:
   - **E-posta/Şifre**: aç (*Email link* kapalı). *Templates* bölümünde gönderen adını "RYVENCA" yap.
   - **Google**: aç, destek e-postasını seç. Google Cloud → *OAuth consent screen*: uygulama adı, logo, gizlilik
     politikası URL'si, yayın durumu *In production*.
   - **Apple**: aç. Services ID'yi ve *OAuth code flow configuration* alanlarını (Team ID, Key ID, `.p8` içeriği)
     gir (bkz. 2. adım). Bu bilgiler hesap silinirken Apple token'ının iptali için de gerekli.
4. **Authentication → Settings → Authorized domains**: web uygulamasının yayınlandığı tüm alan adlarını ekle
   (ör. `ryvenca.com`, `www.ryvenca.com`).
5. Parmak izi ve sağlayıcı ayarları bittikten **sonra** `google-services.json` ve `GoogleService-Info.plist`
   dosyalarını indir. Parmak izi veya sağlayıcı ekledikçe dosyaları yeniden indir.

## 2. Apple Developer

[developer.apple.com/account/resources](https://developer.apple.com/account/resources) → Certificates, Identifiers & Profiles:

1. *Identifiers* → App ID `com.ryvenca.app` → **Sign in with Apple** yeteneğini aç.
2. *Identifiers* → **Services ID** oluştur (ör. `com.ryvenca.web`) → *Sign in with Apple* → *Configure*:
   - Domains: `<project-id>.firebaseapp.com`
   - Return URL: **`https://<project-id>.firebaseapp.com/__/auth/handler`**
3. *Keys* → **+** → *Sign in with Apple* → `.p8` dosyasını indir. Dosya yalnızca bir kez indirilebilir.
   Key ID ve Team ID ile birlikte Firebase'e gir (1.3).

## 3. Backend (sunucu)

1. Service account JSON'u `config/firebase-service-account.json` olarak koy. `docker compose` bu klasörü zaten
   `/app/config` olarak bağlıyor.
2. Ortam değişkenleri:

   | Değişken | Değer |
   |---|---|
   | `RYVENCA_JWT_SECRET` | `openssl rand -base64 48` çıktısı |
   | `RYVENCA_DB_URL` / `_USER` / `_PASSWORD` | PostgreSQL bağlantısı |
   | `RYVENCA_ADMIN_EMAILS` | Senin e-postan (virgülle birden fazla) |
   | `RYVENCA_CORS_ORIGINS` | Web adresi, ör. `https://ryvenca.com` |
   | `RYVENCA_PUBLIC_BASE_URL` | API'nin dış adresi, ör. `https://api.ryvenca.com` |
   | `RYVENCA_LOCAL_AUTH` | Firebase çalıştıktan sonra **`false`** |

3. Başlat ve kontrol et: `GET /api/config` → `auth.firebase: true`.

## 4. Admin paneli (web → `/admin`)

`RYVENCA_ADMIN_EMAILS` içindeki e-postayla bir kez giriş yap. *Yönetim paneli* bağlantısı görünür.
**Genel bakış** sayfasındaki kurulum listesi eksik kalanları gösterir.

**Ayarlar** bölümünde şunları gir:

- **Firebase web yapılandırması**: config kodunu yapıştır → "Alanlara aktar" → Kaydet.
- **Giriş yöntemleri**: Apple / Google / E-posta. Yalnızca açık olanlar gösterilir.
- **Linkler**:
  - Gizlilik politikası, kullanım koşulları, destek sayfası ve destek e-postası.
  - **Hesap silme sayfası**: `https://<web-alan-adın>/delete-account`.
  - App Store ve Google Play adresleri.
- İsteğe bağlı: **Bakım modu** ve **minimum uygulama sürümü**. Minimum sürümün altındaki uygulamalar
  "Uygulamayı güncelle" ekranını gösterir.

Diğer bölümler:

- **Kullanıcılar**: arama, admin yapma, devre dışı bırakma, silme.
- **Silme talepleri**: onay veya ret.
- **Silme kaydı**: anonim silme geçmişi.
- **Paletler**: renk veri seti.

## 5. Web

- Üretim derlemesi: `VITE_API_BASE_URL=https://api.ryvenca.com npm run build` → `dist/` klasörünü yayınla.
- Firebase ayarları derlemeye gömülmez; çalışma anında `/api/config` üzerinden okunur. Yedek yol olarak
  `VITE_FIREBASE_*` değişkenleri de kullanılabilir.

## 6. Mobil (Expo / EAS)

```bash
npm i -g eas-cli && eas login
eas init            # yazdırılan projectId'yi app.config.ts → extra.eas.projectId alanına ekle
npx expo config --type public   # extra.firebaseConfigured: true olmalı
eas build --profile development --platform all   # cihazda test için dev client
```

- `EXPO_PUBLIC_API_BASE_URL` değerini EAS ortam değişkeni (production) olarak ekle.
- Mağaza sürümü: `eas build --profile production --platform all` ve ardından `eas submit`.
- Firebase dosyaları `firebase/` klasöründeyse `eas build` onları yükler. Alternatif olarak EAS dosya değişkenleri
  `GOOGLE_SERVICES_JSON` / `GOOGLE_SERVICE_INFO_PLIST` kullanılabilir (bkz. `ryvenca_mobile/firebase/README.md`).
- Dosyalar yoksa uygulama "fallback" modunda derlenir. Bu modda sunucu izin verirse yerel e-posta/şifre ile
  giriş yapılır.

## 7. Mağaza incelemesi (hesap silme)

| Nereye | Ne girilecek |
|---|---|
| Google Play Console → *Uygulama içeriği* → *Veri güvenliği* → **Hesap silme URL'si** | `https://<web-alan-adın>/delete-account` |
| App Store Connect → *App Privacy* → **Privacy Policy URL** | Gizlilik politikası adresin |
| App Store Connect → *App Review Information* | Demo e-posta/şifre hesabı (Firebase → Authentication → Users → *Add user*) + not: "Hesap silme: Profil → Hesabı Sil" |

Uygulamadaki silme akışları:

- **Uygulama içinde (App Store 5.1.1(v)):** *Profil → Hesabı Sil*.
  1. Silinecekler listelenir.
  2. İstersen neden seçilir.
  3. "Anlıyorum" kutusu işaretlenir.
  4. Son onay verilir.

  Hesap hemen silinir: fotoğraflar, parçalar, kombinler, hesap satırı ve Firebase kullanıcısı. Apple token'ı
  iptal edilir, Google erişimi kaldırılır.
- **Hesaba erişemeyenler:** `/delete-account` sayfası ya da uygulamadaki "Hesap silme talebi gönder". Talep
  admin panelinde onaylanır.
- **Kayıt:** Silinen hesaplardan geriye yalnızca anonim bir kayıt kalır (tuzlanmış e-posta özeti, sağlayıcı,
  yöntem, tarih).

## 8. Son test (gerçek cihazda)

Bu adımlar Firebase projesi ve cihaz gerektirdiği için geliştirme ortamında doğrulanamadı:

- [ ] iOS: Apple ile giriş (ilk girişte ad geliyor mu?), Google ile giriş, e-posta ile kayıt + doğrulama e-postası
      + şifre sıfırlama
- [ ] Android: Google ile giriş (SHA parmak izleri doğruysa çalışır), e-posta ile giriş
- [ ] Web: Apple / Google açılır pencere girişi, e-posta ile giriş
- [ ] Her sağlayıcıyla hesap silme; ardından Firebase → Users listesinde kullanıcının kalmadığını kontrol et
- [ ] `/delete-account` sayfasından talep gönder → admin panelinde onayla
