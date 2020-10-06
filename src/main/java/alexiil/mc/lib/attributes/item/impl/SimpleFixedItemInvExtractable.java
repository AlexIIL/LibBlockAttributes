/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** @deprecated Use {@link GroupedItemInvFixedWrapper} instead of this! */
@Deprecated // (since = "0.4.0", forRemoval = true)
public final class SimpleFixedItemInvExtractable implements ItemExtractable {

    private final GroupedItemInvFixedWrapper wrapper;

    public SimpleFixedItemInvExtractable(FixedItemInv inv) {
        this.wrapper = new GroupedItemInvFixedWrapper(inv);
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxCount, Simulation simulation) {
        return wrapper.attemptExtraction(filter, maxCount, simulation);
    }
}
