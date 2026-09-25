package com.ryvenca.outfit.engine;

import static com.ryvenca.common.TurkishText.capitalize;
import static com.ryvenca.common.TurkishText.joinAnd;
import static com.ryvenca.common.TurkishText.lower;

import java.util.ArrayList;
import java.util.List;

import com.ryvenca.catalog.Occasion;
import com.ryvenca.catalog.Season;
import com.ryvenca.catalog.StylePreference;
import com.ryvenca.catalog.Texture;
import com.ryvenca.color.ColorName;
import com.ryvenca.color.PaletteLibrary;
import com.ryvenca.outfit.engine.OutfitStory.Reason;

/** Turns the numeric analyses into Turkish copy: title, description and the "Neden Uyumlu?" cards. */
public final class OutfitExplainer {

    private final PaletteLibrary palettes;

    public OutfitExplainer(PaletteLibrary palettes) {
        this.palettes = palettes;
    }

    public OutfitStory explain(OutfitEvaluation e) {
        return explain(e, java.util.Set.of());
    }

    /**
     * @param usedTitles titles already shown in the same list; another fitting variant is chosen when
     *                   possible so two cards next to each other do not carry the same name.
     */
    public OutfitStory explain(OutfitEvaluation e, java.util.Set<String> usedTitles) {
        String paletteName = e.color().paletteMatched() ? palettes.get(e.color().paletteIndex()).name() : null;
        List<Reason> reasons = new ArrayList<>();
        reasons.add(new Reason("COLOR", "Renk Dengesi", colorText(e.color(), paletteName)));
        reasons.add(new Reason("TEXTURE", "Doku Uyumu", textureText(e)));
        reasons.add(new Reason("OCCASION", "Kullanım Alanı", occasionText(e.occasion())));
        String season = seasonText(e.season());
        if (season != null) {
            reasons.add(new Reason("SEASON", "Mevsim", season));
        }
        List<String[]> options = titles(e);
        String[] title = options.stream().filter(t -> !usedTitles.contains(t[0])).findFirst().orElse(options.getFirst());
        return new OutfitStory(title[0], title[1], List.copyOf(reasons), paletteName);
    }

    // ---- Renk Dengesi -------------------------------------------------------------------------

    static String colorText(ColorAnalysis c, String paletteName) {
        List<String> all = labels(c.colors());
        String text = switch (c.colorCase()) {
            case SINGLE_COLOR -> "Baştan aşağı " + lower(c.colors().getFirst().label())
                    + ": tek renkle giyinmek silueti uzatır ve görünümü bütünlüklü kılar.";
            case MONOCHROME -> capitalize(joinAnd(all))
                    + " net bir kontrastla grafik ve modern bir monokrom görünüm yaratıyor.";
            case TONAL -> capitalize(joinAnd(all))
                    + " aynı renk ailesinin farklı tonları; ton sür ton katmanlar sofistike bir derinlik katıyor.";
            case ALL_NEUTRAL -> capitalize(joinAnd(all))
                    + ": nötr tonlar sakin ve dengeli bir palet oluşturuyor; zamansız ve kolay bir uyum.";
            case SINGLE_ACCENT -> c.neutrals().isEmpty()
                    ? capitalize(lower(c.accents().getFirst().label())) + " kombinin odak noktası; diğer parçalar onu sade bir şekilde dengeliyor."
                    : capitalize(joinAnd(labels(c.neutrals()))) + " nötr bir taban kuruyor; "
                            + lower(c.accents().getFirst().label()) + " ise kombine kontrollü bir canlılık katıyor.";
            case SOFT_PAIR -> capitalize(joinAnd(labels(c.accents())))
                    + " yumuşak tonlarıyla ferah ve hafif bir uyum yakalıyor.";
            case ANALOGOUS -> capitalize(joinAnd(labels(c.accents())))
                    + " renk çemberinde komşu tonlar; birlikte yumuşak ve uyumlu bir geçiş sağlıyor.";
            case COMPLEMENTARY -> capitalize(joinAnd(labels(c.accents())))
                    + " birbirini tamamlayan zıt renkler; cesur ama dengeli bir kontrast oluşturuyor.";
            case DISCORDANT -> capitalize(joinAnd(labels(c.accents())))
                    + " birlikte biraz iddialı duruyor; nötr bir parça bu ikiliyi dengeleyebilir.";
            case BUSY -> capitalize(joinAnd(all))
                    + " bir arada oldukça hareketli; parçalardan birini nötr bir tonla değiştirmek uyumu artırır.";
        };
        StringBuilder sb = new StringBuilder(text);
        if (paletteName != null) {
            sb.append(" Renkler \"").append(paletteName).append("\" paletimize çok yakın.");
        }
        if (c.accentEcho()) {
            sb.append(" Aynı rengin iki parçada tekrar etmesi kombini bir arada tutuyor.");
        } else if (c.bagShoeEcho()) {
            sb.append(" Çanta ile ayakkabının uyumu görünümü tamamlıyor.");
        }
        if (c.nearClash()) {
            sb.append(" Üst ve alt parçanın tonları birbirine çok yakın ama aynı değil; daha net bir kontrast daha iyi olabilir.");
        }
        if (c.patternClash()) {
            sb.append(" İki desenli parça aynı anda dikkat çekmek için yarışıyor.");
        }
        return sb.toString();
    }

    // ---- Doku Uyumu ---------------------------------------------------------------------------

    static String textureText(OutfitEvaluation e) {
        CompatibilityAnalysis c = e.compatibility();
        List<WardrobeItem> p = c.pairingItems();
        String main;
        if (c.pairing() != null) {
            main = switch (c.pairing()) {
                case SUIT -> capitalize(p.get(0).phrase()) + " ile " + p.get(1).phrase()
                        + " takım etkisi yaratarak güçlü ve derli toplu bir siluet kuruyor.";
                case SMART_MIX -> capitalize(p.get(0).phrase()) + " ile " + p.get(1).phrase()
                        + ", klasik ile rahatı dengeleyen modern bir ikili.";
                case EDGY_CONTRAST -> capitalize(p.get(0).phrase()) + " ile " + p.get(1).phrase()
                        + ": sert deri ile akışkan kumaşın kontrastı kombine karakter katıyor.";
                case CLASSIC_BASE -> capitalize(p.get(0).phrase()) + " ile " + p.get(1).phrase()
                        + " klasik ve güvenilir bir temel oluşturuyor.";
                case KNIT_CONTRAST -> capitalize(p.get(0).phrase()) + " ile " + p.get(1).phrase()
                        + ": yumuşak triko ile düzgün kesimli alt parça birbirini dengeliyor.";
                case EASY_CLASSIC -> "T-shirt, jean ve sneaker; zahmetsiz ama her zaman işe yarayan klasik bir üçlü.";
                case TRENCH -> capitalize(p.get(0).phrase())
                        + " kombine zamansız ve derli toplu bir dış katman ekliyor.";
            };
            if (c.textures().size() >= 2 && c.pairing() != CompatibilityAnalysis.Pairing.EASY_CLASSIC) {
                main += " Farklı dokular (" + joinAnd(textureLabels(c.textures())) + ") görünüme derinlik katıyor.";
            }
        } else if (c.textures().size() >= 2) {
            List<String> names = e.items().stream()
                    .filter(i -> i.role().isCore() && i.subcategory().texture() != null)
                    .map(i -> lower(i.subcategory().label()))
                    .distinct()
                    .toList();
            main = capitalize(joinAnd(names)) + " farklı dokularıyla (" + joinAnd(textureLabels(c.textures()))
                    + ") zengin ve katmanlı bir görünüm sunuyor.";
        } else if (c.textures().size() == 1) {
            main = "Parçaların benzer " + c.textures().getFirst().label()
                    + " dokusu sade ve bütünlüklü bir görünüm sağlıyor.";
        } else {
            main = "Parçaların kesimleri ve dokuları birbiriyle uyumlu, derli toplu bir görünüm sağlıyor.";
        }
        if (!c.mismatch().isEmpty()) {
            WardrobeItem formal = c.mismatch().get(0);
            WardrobeItem casual = c.mismatch().get(1);
            // Suggest replacing the piece furthest from the outfit's overall formality; on a tie the
            // smaller piece (usually the shoes) is the easier swap.
            double formalGap = Math.abs(formal.formality() - c.formality());
            double casualGap = Math.abs(casual.formality() - c.formality());
            WardrobeItem odd = Math.abs(formalGap - casualGap) < 0.3
                    ? (formal.role().visualArea() <= casual.role().visualArea() ? formal : casual)
                    : (formalGap > casualGap ? formal : casual);
            main += " " + capitalize(formal.phrase()) + " ile " + casual.phrase()
                    + " arasındaki resmiyet farkı belirgin; daha uyumlu bir " + lower(odd.role().label())
                    + " seçimi kombini güçlendirebilir.";
        } else if (!c.highLow().isEmpty() && c.pairing() != CompatibilityAnalysis.Pairing.SMART_MIX) {
            main += " " + capitalize(c.highLow().get(0).phrase()) + " ile " + c.highLow().get(1).phrase()
                    + " şık ve rahatı dengeleyen modern bir karışım oluşturuyor.";
        }
        return main;
    }

    private static List<String> textureLabels(List<Texture> textures) {
        return textures.stream().map(Texture::label).toList();
    }

    // ---- Kullanım Alanı -----------------------------------------------------------------------

    static String occasionText(OccasionAnalysis o) {
        StringBuilder sb = new StringBuilder();
        Occasion requested = o.requested();
        if (requested != null && o.scores().get(requested) < 0.6) {
            boolean tooCasual = o.formality() < requested.minFormality();
            sb.append(capitalize(occasionWord(requested))).append(" için biraz ")
                    .append(tooCasual ? "rahat" : "resmi").append(" kalabilir. ");
        }
        List<Occasion> occasions = o.occasions();
        if (occasions.size() >= 2) {
            sb.append("Hem ").append(occasionWord(occasions.get(0))).append(" hem de ")
                    .append(occasionWord(occasions.get(1))).append(" ortamlarında rahatlıkla tercih edilebilir.");
        } else {
            sb.append(switch (o.primary()) {
                case OFFICE -> "Resmiyet dengesi ofis ve iş ortamları için ideal.";
                case DAILY -> "Gün içinde rahat hareket etmeni sağlarken özenli görünmeni sağlar.";
                case EVENING -> "Akşam planları için şık ve iddialı bir seçim.";
                case WEEKEND -> "Hafta sonu için rahat ve zahmetsiz bir seçim.";
                case SPORT -> "Hareket özgürlüğü sağlayan parçalarla aktif günler için uygun.";
            });
        }
        if (!o.venues().isEmpty()) {
            sb.append(' ').append(capitalize(joinAnd(o.venues().stream().map(v -> lower(v)).toList())))
                    .append(" için uygun.");
        }
        return sb.toString();
    }

    private static String occasionWord(Occasion occasion) {
        return switch (occasion) {
            case DAILY -> "günlük";
            case OFFICE -> "ofis";
            case EVENING -> "akşam";
            case WEEKEND -> "hafta sonu";
            case SPORT -> "spor";
        };
    }

    // ---- Mevsim -------------------------------------------------------------------------------

    static String seasonText(SeasonAnalysis s) {
        String season = lower(s.season().label());
        return switch (s.warmth()) {
            case TOO_LIGHT -> "Bu kombin " + season + " için biraz ince kalabilir; üzerine sıcak bir katman eklemeyi düşünebilirsin.";
            case TOO_WARM -> "Bu kombin " + season + " için biraz kalın kalabilir; daha hafif bir katman tercih edebilirsin.";
            case OK -> s.layer() == null ? null
                    : capitalize(season) + " için uygun: " + s.layer().phrase()
                            + (s.season() == Season.WINTER ? " soğuk günlerde sıcak tutan şık bir katman sağlıyor."
                            : " serin havalarda şık bir katman sağlıyor.");
        };
    }

    // ---- Başlık -------------------------------------------------------------------------------

    /** Fitting title/description pairs, preferred first (the preference varies per outfit). */
    static List<String[]> titles(OutfitEvaluation e) {
        int variant = Math.floorMod(e.key().hashCode(), 3);
        StylePreference style = e.style().style();
        Occasion primary = e.occasion().primary();
        ColorAnalysis.ColorCase colorCase = e.color().colorCase();
        if (primary == Occasion.EVENING && style != StylePreference.SPORT) {
            return pick(variant,
                    new String[] {"Şehirde Akşam", "Gün boyundan geceye kolayca uyarlanabilen zamansız parçalar."},
                    new String[] {"Akşam Şıklığı", "Akşam planları için sade ama etkileyici bir görünüm."},
                    new String[] {"Gece Zarafeti", "Işıltıya ihtiyaç duymadan şık ve kendinden emin."});
        }
        if (colorCase == ColorAnalysis.ColorCase.TONAL && variant == 0) {
            List<String[]> tonal = new ArrayList<>();
            tonal.add(new String[] {"Ton Sür Ton", "Aynı renk ailesinin tonlarıyla sofistike ve bütünlüklü bir görünüm."});
            tonal.addAll(byStyle(e, 1));
            return tonal;
        }
        return byStyle(e, variant);
    }

    private static List<String[]> byStyle(OutfitEvaluation e, int variant) {
        StylePreference style = e.style().style();
        Occasion primary = e.occasion().primary();
        return switch (style) {
            case CLASSIC -> primary == Occasion.OFFICE
                    ? pick(variant,
                            new String[] {"Ofis Şıklığı", "Klasik parçalarla modern bir ofis görünümü."},
                            new String[] {"Zamansız Şıklık", "Klasik parçalarla modern ve sade bir görünüm."},
                            new String[] {"Klasik Denge", "Her ortamda şıklığını koruyan zamansız bir seçim."})
                    : pick(variant,
                            new String[] {"Zamansız Şıklık", "Klasik parçalarla modern ve sade bir görünüm."},
                            new String[] {"Klasik Denge", "Her ortamda şıklığını koruyan zamansız bir seçim."},
                            new String[] {"Sade Zarafet", "Az ama doğru parçayla zahmetsiz bir şıklık."});
            case SMART_CASUAL -> pick(variant,
                    new String[] {"Smart Casual", "Klasik ve rahat parçaları bir araya getirerek günlük şıklık yakala."},
                    new String[] {"Rahat Şıklık", "Özenli görünürken rahat hissettiren dengeli bir kombin."},
                    new String[] {"Özenli Rahatlık", "Gün boyu rahat, her an şık."});
            case MINIMAL -> primary == Occasion.OFFICE
                    ? pick(0, new String[] {"Minimal Ofis", "Sade parçalarla modern bir ofis görünümü."},
                            new String[] {"Sade ve Net", "Az renk, temiz çizgiler, güçlü bir duruş."})
                    : pick(variant,
                            new String[] {"Minimal Denge", "Sade parçalarla modern ve net bir görünüm."},
                            new String[] {"Sade ve Net", "Az renk, temiz çizgiler, güçlü bir duruş."},
                            e.compatibility().formality() >= 3.2
                                    ? new String[] {"Sessiz Lüks", "Nötr tonlar ve temiz kesimlerle zahmetsiz bir şıklık."}
                                    : new String[] {"Sade Rahatlık", "Nötr tonlarda, rahat ve derli toplu bir görünüm."});
            case BUSINESS -> pick(variant,
                    new String[] {"Toplantı Günü", "Profesyonel ve kendinden emin bir görünüm."},
                    new String[] {"Güçlü Duruş", "Net çizgilerle profesyonel bir şıklık."},
                    new String[] {"İş Şıklığı", "Toplantıdan iş yemeğine rahatlıkla geçebilen bir kombin."});
            case STREETWEAR -> pick(variant,
                    new String[] {"Sokak Stili", "Rahat kesimler ve güçlü parçalarla şehirli bir görünüm."},
                    new String[] {"Şehir Ritmi", "Şehrin temposuna uyan rahat ve karakterli bir kombin."},
                    new String[] {"Rahat ve Cool", "Zahmetsiz ama dikkat çeken bir sokak görünümü."});
            case SPORT -> pick(variant,
                    new String[] {"Aktif Gün", "Konforlu ve fonksiyonel parçalarla enerjik bir görünüm."},
                    new String[] {"Hareket Özgürlüğü", "Spor ve aktif günler için rahat bir kombin."},
                    new String[] {"Sportif Denge", "Rahat ama derli toplu bir spor görünümü."});
            case CASUAL -> primary == Occasion.WEEKEND
                    ? pick(variant,
                            new String[] {"Hafta Sonu Rahat", "Günlük konfor, zamansız stil."},
                            new String[] {"Pazar Keyfi", "Rahat, sade ve her zaman işe yarayan bir kombin."},
                            new String[] {"Hafta Sonu Kaçamağı", "Gezmeye, kahveye ve uzun yürüyüşlere hazır."})
                    : pick(variant,
                            new String[] {"Günlük Rahatlık", "Gün boyu rahat ve derli toplu bir görünüm."},
                            new String[] {"Kolay Şıklık", "Zahmetsiz ama özenli günlük bir kombin."},
                            new String[] {"Her Günün Kombini", "Dolabındaki temel parçalarla güvenli bir seçim."});
        };
    }

    /** All options, starting with {@code variant} and wrapping around. */
    private static List<String[]> pick(int variant, String[]... options) {
        List<String[]> ordered = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            ordered.add(options[(variant + i) % options.length]);
        }
        return ordered;
    }

    private static List<String> labels(List<ColorName> colors) {
        return colors.stream().map(c -> lower(c.label())).toList();
    }
}
