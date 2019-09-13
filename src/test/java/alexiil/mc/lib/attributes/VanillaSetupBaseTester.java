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

import org.junit.BeforeClass;

import net.minecraft.Bootstrap;

public class VanillaSetupBaseTester {

    @BeforeClass
    public static void init() {
        System.out.println("INIT");
        PrintStream sysOut = System.out;
        InputStream sysIn = System.in;

        Bootstrap.initialize();

        System.setIn(sysIn);
        System.setOut(sysOut);
    }

}
