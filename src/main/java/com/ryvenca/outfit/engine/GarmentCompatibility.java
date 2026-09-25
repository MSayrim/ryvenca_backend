package com.ryvenca.outfit.engine;

import static com.ryvenca.catalog.Subcategory.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ryvenca.catalog.Subcategory;
import com.ryvenca.catalog.Texture;
import com.ryvenca.color.ColorScience;
import com.ryvenca.outfit.engine.CompatibilityAnalysis.Pairing;

/**
 * Garment/category compatibility: formality coherence between pieces, fabric (texture) variety,
 * known clashes (e.g. sweatpants with loafers) and known good pairings (e.g. blazer with jeans).
 */
final class GarmentCompatibility {

    private record Clash(Subcategory piece, Set<Subcategory> with, double factor) {
    }

    private static final List<Clash> CLASHES = List.of(
            new Clash(SWEATPANTS, EnumSet.of(LOAFER, DRESS_SHOE, HEELS, BLAZER, COAT, TRENCH_COAT, CLUTCH, HANDBAG, SHIRT, BLOUSE), 0.55),
            new Clash(RUNNING_SHOE, EnumSet.of(TROUSERS, SKIRT, EVENING_DRESS, SLIP_DRESS, SHIRT_DRESS, BLAZER, COAT, CLUTCH, BLOUSE), 0.6),
            new Clash(EVENING_DRESS, EnumSet.of(SNEAKER, RUNNING_SHOE, BACKPACK, PUFFER, HOODIE, CROSSBODY, DENIM_JACKET), 0.5),
            new Clash(SLIP_DRESS, EnumSet.of(RUNNING_SHOE, BACKPACK, PUFFER), 0.65),
            new Clash(HOODIE, EnumSet.of(BLAZER, COAT, TRENCH_COAT, HEELS, DRESS_SHOE, CLUTCH, SKIRT, TROUSERS), 0.72),
            new Clash(SHORTS, EnumSet.of(BOOT, COAT, PUFFER, DRESS_SHOE, HEELS), 0.65),
            new Clash(SANDAL, EnumSet.of(COAT, PUFFER, SWEATPANTS, KNIT_DRESS, SWEATER), 0.7),
            new Clash(BACKPACK, EnumSet.of(HEELS, DRESS_SHOE, BLAZER), 0.8),
            new Clash(CLUTCH, EnumSet.of(SNEAKER, RUNNING_SHOE, SWEATSHIRT, SHORTS, T_SHIRT), 0.82),
            new Clash(TANK_TOP, EnumSet.of(COAT, TRENCH_COAT, TROUSERS), 0.8),
            new Clash(DRESS_SHOE, EnumSet.of(JEANS, SHORTS, T_SHIRT, SWEATSHIRT), 0.8));

    private GarmentCompatibility() {
    }

    static CompatibilityAnalysis analyze(List<WardrobeItem> items) {
        List<WardrobeItem> relevant = items.stream().filter(i -> i.role() != OutfitRole.ACCESSORY).toList();

        double formality = 0;
        double totalWeight = 0;
        for (WardrobeItem item : relevant) {
            double w = weight(item);
            formality += item.formality() * w;
            totalWeight += w;
        }
        formality /= totalWeight;

        double pairScores = 0;
        double pairWeights = 0;
        List<WardrobeItem> highLow = List.of();
        List<WardrobeItem> mismatch = List.of();
        double worstGap = 0;
        for (int i = 0; i < relevant.size(); i++) {
            for (int j = i + 1; j < relevant.size(); j++) {
                WardrobeItem a = relevant.get(i);
                WardrobeItem b = relevant.get(j);
                double gap = Math.abs(a.formality() - b.formality());
                double penalty = clamp((gap - 1.5) / 2.5);
                boolean shoes = a.role() == OutfitRole.SHOES || b.role() == OutfitRole.SHOES;
                double w = weight(a) * weight(b) * (shoes ? 1.2 : 1.0);
                pairScores += w * (1 - penalty);
                pairWeights += w;
                boolean garments = a.role() != OutfitRole.BAG && b.role() != OutfitRole.BAG;
                if (garments && gap >= 1.5 && gap <= 2.6 && Math.max(a.formality(), b.formality()) >= 4 && highLow.isEmpty()) {
                    highLow = a.formality() >= b.formality() ? List.of(a, b) : List.of(b, a);
                }
                if (gap > 2.6 && gap > worstGap) {
                    worstGap = gap;
                    mismatch = a.formality() >= b.formality() ? List.of(a, b) : List.of(b, a);
                }
            }
        }
        double formalityScore = pairWeights == 0 ? 1 : pairScores / pairWeights;

        Set<Subcategory> subs = EnumSet.noneOf(Subcategory.class);
        items.forEach(i -> subs.add(i.subcategory()));
        double factor = 1;
        for (Clash clash : CLASHES) {
            if (subs.contains(clash.piece()) && clash.with().stream().anyMatch(subs::contains)) {
                factor *= clash.factor();
            }
        }
        WardrobeItem jeans = find(items, JEANS);
        WardrobeItem denimJacket = find(items, DENIM_JACKET);
        if (jeans != null && denimJacket != null && ColorScience.deltaE2000(jeans.lab(), denimJacket.lab()) < 10) {
            factor *= 0.82;
        }

        Set<Texture> textures = new LinkedHashSet<>();
        relevant.stream()
                .filter(i -> i.role().isCore())
                .sorted((x, y) -> Double.compare(y.role().visualArea(), x.role().visualArea()))
                .forEach(i -> {
                    if (i.subcategory().texture() != null) {
                        textures.add(i.subcategory().texture());
                    }
                });
        double textureScore = switch (textures.size()) {
            case 0 -> 0.85;
            case 1 -> 0.78;
            case 2 -> 0.95;
            default -> 1.0;
        };

        List<WardrobeItem> pairingItems = new ArrayList<>();
        Pairing pairing = pairing(items, pairingItems);
        double bonus = switch (pairing) {
            case null -> 0;
            case SUIT, SMART_MIX, EDGY_CONTRAST -> 0.05;
            case CLASSIC_BASE, KNIT_CONTRAST, EASY_CLASSIC -> 0.04;
            case TRENCH -> 0.02;
        };
        if (pairing == Pairing.SUIT) {
            textureScore = 0.95;
        }

        double score = clamp(0.72 * formalityScore + 0.28 * textureScore + bonus) * factor;
        if (!mismatch.isEmpty() && pairing == Pairing.SMART_MIX) {
            mismatch = List.of();
        }
        return new CompatibilityAnalysis(clamp(score), formality, List.copyOf(textures), pairing,
                List.copyOf(pairingItems), highLow, mismatch);
    }

    private static Pairing pairing(List<WardrobeItem> items, List<WardrobeItem> out) {
        WardrobeItem blazer = find(items, BLAZER);
        WardrobeItem trousers = find(items, TROUSERS);
        WardrobeItem jeans = find(items, JEANS);
        if (blazer != null && trousers != null && ColorScience.deltaE2000(blazer.lab(), trousers.lab()) < 6) {
            out.addAll(List.of(blazer, trousers));
            return Pairing.SUIT;
        }
        if (blazer != null && jeans != null) {
            out.addAll(List.of(blazer, jeans));
            return Pairing.SMART_MIX;
        }
        WardrobeItem leather = find(items, LEATHER_JACKET);
        WardrobeItem soft = firstOf(items, DAY_DRESS, SLIP_DRESS, SKIRT);
        if (leather != null && soft != null) {
            out.addAll(List.of(leather, soft));
            return Pairing.EDGY_CONTRAST;
        }
        WardrobeItem shirt = firstOf(items, SHIRT, BLOUSE);
        if (shirt != null && trousers != null) {
            out.addAll(List.of(shirt, trousers));
            return Pairing.CLASSIC_BASE;
        }
        WardrobeItem knit = firstOf(items, SWEATER, CARDIGAN);
        WardrobeItem tailored = firstOf(items, TROUSERS, SKIRT, CHINOS);
        if (knit != null && tailored != null) {
            out.addAll(List.of(knit, tailored));
            return Pairing.KNIT_CONTRAST;
        }
        WardrobeItem tee = find(items, T_SHIRT);
        WardrobeItem sneaker = find(items, SNEAKER);
        if (tee != null && jeans != null && sneaker != null) {
            out.addAll(List.of(tee, jeans, sneaker));
            return Pairing.EASY_CLASSIC;
        }
        WardrobeItem trench = find(items, TRENCH_COAT);
        if (trench != null) {
            out.add(trench);
            return Pairing.TRENCH;
        }
        return null;
    }

    private static double weight(WardrobeItem item) {
        return item.role() == OutfitRole.BAG ? 0.5 : 1.0;
    }

    private static WardrobeItem find(List<WardrobeItem> items, Subcategory sub) {
        for (WardrobeItem item : items) {
            if (item.subcategory() == sub) {
                return item;
            }
        }
        return null;
    }

    private static WardrobeItem firstOf(List<WardrobeItem> items, Subcategory... subs) {
        for (Subcategory sub : subs) {
            WardrobeItem item = find(items, sub);
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
