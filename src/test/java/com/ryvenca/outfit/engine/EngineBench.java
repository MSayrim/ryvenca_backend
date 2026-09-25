package com.ryvenca.outfit.engine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.ryvenca.catalog.Category;
import com.ryvenca.catalog.Season;
import com.ryvenca.catalog.Subcategory;
import com.ryvenca.color.ColorName;
import com.ryvenca.color.Lab;
import com.ryvenca.color.PaletteLibrary;

class EngineBench {

    @Test
    @EnabledIfSystemProperty(named = "bench", matches = "true")
    void bench() {
        Random r = new Random(1);
        List<WardrobeItem> items = new ArrayList<>();
        int[] counts = {60, 45, 25, 15, 25, 15, 15};
        long id = 1;
        for (Category c : Category.values()) {
            List<Subcategory> subs = Subcategory.of(c);
            for (int i = 0; i < counts[c.ordinal()]; i++) {
                Subcategory s = subs.get(r.nextInt(subs.size()));
                ColorName color = ColorName.values()[r.nextInt(ColorName.values().length)];
                items.add(new WardrobeItem(id++, s, color, color.lab(), r.nextInt(8) == 0, EnumSet.copyOf(s.defaultSeasons()),
                        EnumSet.copyOf(s.defaultOccasions())));
            }
        }
        OutfitRequest req = new OutfitRequest(Season.AUTUMN, null, Set.of(), 1);
        for (int round = 0; round < 5; round++) {
            long t0 = System.nanoTime();
            OutfitEngine engine = new OutfitEngine(items, new PaletteLibrary());
            long t1 = System.nanoTime();
            int ready = engine.countReady(req, 70, 99);
            long t2 = System.nanoTime();
            var s = engine.suggest(req, 10);
            long t3 = System.nanoTime();
            var p = engine.pairings(items.get(0), req, 6, 6);
            long t4 = System.nanoTime();
            System.out.printf("items=%d build=%dms ready(%d)=%dms suggest(%d)=%dms pairings=%dms%n", items.size(),
                    (t1 - t0) / 1_000_000, ready, (t2 - t1) / 1_000_000, s.size(), (t3 - t2) / 1_000_000, (t4 - t3) / 1_000_000);
        }
    }
}
