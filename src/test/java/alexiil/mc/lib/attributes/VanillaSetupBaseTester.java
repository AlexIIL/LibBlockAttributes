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

import org.junit.BeforeClass;

import net.minecraft.Bootstrap;
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
        map.put("libblockattributes.fluid.flow_rate", "%2$s per %1$s");
        map.put("libblockattributes.fluid.tank_empty", "Empty %s Tank");
        map.put("libblockattributes.fluid.tank_full", "Full %s Tank");
        map.put("libblockattributes.fluid.tank_partial", "%2$s / %3$s %1$s Tank");
        map.put("libblockattributes.fluid.tank_multi_unit", "%2$s in a %1s Tank");
        map.put("libblockattributes.fluid.multi_unit.2", "%1$s %2$s and %3$s %4$s");
        map.put("libblockattributes.fluid.multi_unit.3", "%1$s %2$s, %3$s %4$s, and %5$s %6$s");
        map.put("libblockattributes.fluid.multi_unit.4", "%1$s %2$s, %3$s %4$s, %5$s %6$s, and %7$s %8$s");
        map.put("libblockattributes.fluid.multi_unit.combiner", "%1$s, %2$s %3$s");
        map.put("libblockattributes.fluid.multi_unit.end", "%1$s, and %2$s %3$s");
        map.put("libblockattributes.time_unit.tick", "Tick");
        map.put("libblockattributes.time_unit.second", "Second");
        map.put("libblockattributes.time_unit.minute", "Minute");
        map.put("libblockattributes.fluid_unit.bucket.singular", "Bucket");
        map.put("libblockattributes.fluid_unit.bucket.plural", "Buckets");
        map.put("libblockattributes.fluid_unit.bottle.singular", "Bottle");
        map.put("libblockattributes.fluid_unit.bottle.plural", "Bottles");

        // And some custom ones

        map.put("libblockattributes.fluid_unit.block.singular", "Block");
        map.put("libblockattributes.fluid_unit.block.plural", "Blocks");
        map.put("libblockattributes.fluid_unit.ingot.singular", "Ingot");
        map.put("libblockattributes.fluid_unit.ingot.plural", "Ingots");
        map.put("libblockattributes.fluid_unit.nugget.singular", "Nugget");
        map.put("libblockattributes.fluid_unit.nugget.plural", "Nuggets");

        Language.load(map);
    }
}
