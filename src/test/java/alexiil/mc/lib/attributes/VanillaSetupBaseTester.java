/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.BeforeClass;

import net.minecraft.Bootstrap;
import net.minecraft.client.font.TextVisitFactory;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Language;

public class VanillaSetupBaseTester {

    private static boolean hasSetup = false;

    @BeforeClass
    public static void init() {

        if (hasSetup) {
            return;
        }
        hasSetup = true;

        PrintStream sysOut = System.out;
        InputStream sysIn = System.in;

        Bootstrap.initialize();

        System.setIn(sysIn);
        System.setOut(sysOut);

        Map<String, String> map = new HashMap<>();

        // For testing purposes, copied from en_us.json
        map.put("libblockattributes.fluid.amount", "%1$s %2$s");
        map.put("libblockattributes.fluid.name", "%1$s of %2$s");
        map.put("libblockattributes.fluid.tank_empty.short", "0/%1$s");
        map.put("libblockattributes.fluid.tank_empty.long", "Empty %s Tank");
        map.put("libblockattributes.fluid.tank_full.short", "%1$s/%1$s");
        map.put("libblockattributes.fluid.tank_full.long", "Full %1$s Tank");
        map.put("libblockattributes.fluid.tank_partial.short", "%1$s/%2$s");
        map.put("libblockattributes.fluid.tank_partial.long", "%1$s in a %2$s Tank");
        map.put("libblockattributes.fluid.tank_multi_unit.short", "%1$s/%2$s");
        map.put("libblockattributes.fluid.tank_multi_unit.long", "%1$s in a %2$s Tank");
        map.put("libblockattributes.fluid.multi_unit.2", "%1$s %2$s and %3$s %4$s");
        map.put("libblockattributes.fluid.multi_unit.3", "%1$s %2$s, %3$s %4$s, and %5$s %6$s");
        map.put("libblockattributes.fluid.multi_unit.4", "%1$s %2$s, %3$s %4$s, %5$s %6$s, and %7$s %8$s");
        map.put("libblockattributes.fluid.multi_unit.combiner", "%1$s %2$s %3$s");
        map.put("libblockattributes.fluid.multi_unit.end", "%1$s and %2$s %3$s");
        map.put("libblockattributes.fluid.flow_rate.short", "%1$s/%2$s");
        map.put("libblockattributes.fluid.flow_rate.long", "%1$s per %2$s");
        map.put("libblockattributes.time_unit.tick.singular", "Tick");
        map.put("libblockattributes.time_unit.tick.plural", "Ticks");
        map.put("libblockattributes.time_unit.tick.symbol", "t");
        map.put("libblockattributes.time_unit.second.singular", "Second");
        map.put("libblockattributes.time_unit.second.plural", "Seconds");
        map.put("libblockattributes.time_unit.second.symbol", "s");
        map.put("libblockattributes.fluid_unit.bucket.singular", "Bucket");
        map.put("libblockattributes.fluid_unit.bucket.plural", "Buckets");
        map.put("libblockattributes.fluid_unit.bucket.symbol", "B");
        map.put("libblockattributes.fluid_unit.bottle.singular", "Bottle");
        map.put("libblockattributes.fluid_unit.bottle.plural", "Bottles");
        map.put("libblockattributes.fluid_unit.bottle.symbol", "b");
        map.put("libblockattributes.fluid_property.advanced_prefix_key", "Property, %1$s");
        map.put("libblockattributes.fluid_property.advanced_prefix_value", "Value, %1$s");

        // And some custom ones

        map.put("libblockattributes.fluid_unit.block.singular", "Block");
        map.put("libblockattributes.fluid_unit.block.plural", "Blocks");
        map.put("libblockattributes.fluid_unit.ingot.singular", "Ingot");
        map.put("libblockattributes.fluid_unit.ingot.plural", "Ingots");
        map.put("libblockattributes.fluid_unit.nugget.singular", "Nugget");
        map.put("libblockattributes.fluid_unit.nugget.plural", "Nuggets");

        // Some that vanilla needs

        map.put("block.minecraft.water", "Water");
        map.put("block.minecraft.lava", "Lava");
        map.put("item.minecraft.potion.effect.healing", "Healing Potion");

        Language.setInstance(new Language() {
            @Override
            public boolean isRightToLeft() {
                return false;
            }

            @Override
            public OrderedText reorder(StringVisitable text) {
                return (visitor) -> {
                    return text.visit((style, string) -> {
                        if (TextVisitFactory.visitFormatted(string, style, visitor)) {
                            return Optional.empty();
                        } else {
                            return StringVisitable.TERMINATE_VISIT;
                        }
                    }, Style.EMPTY).isPresent();
                };
            }

            @Override
            public boolean hasTranslation(String key) {
                if (!map.containsKey(key)) {
                    Assert.fail("No translation for '" + key + "'");
                }
                return map.containsKey(key);
            }

            @Override
            public String get(String key) {
                if (!map.containsKey(key)) {
                    Assert.fail("No translation for '" + key + "'");
                }
                return map.getOrDefault(key, key);
            }
        });
    }
}
