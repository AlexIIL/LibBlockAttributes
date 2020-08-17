/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import net.fabricmc.api.ModInitializer;

import alexiil.mc.lib.attributes.item.impl.DirectFixedItemInvTester;

public class Tester implements ModInitializer {
    @Override
    public void onInitialize() {
        DirectFixedItemInvTester.runTests();

        // TODO: Write tests for SimpleLimitedFixedItemInv and SimpleGroupedItemInv!
    }
}
