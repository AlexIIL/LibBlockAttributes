/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;

/** Various utilities for dealing with fluids contained in single {@link ItemStack}s. */
public final class FluidItemUtil {
    private FluidItemUtil() {}

    /** Retrieves the first fluid contained by the given {@link ItemStack}.
     * 
     * @return The {@link FluidKey} if the {@link ItemStack} contained any, or {@link FluidKeys#EMPTY} if none were
     *         present. */
    public static FluidKey getContainedFluid(ItemStack stack) {
        GroupedFluidInvView inv = FluidAttributes.GROUPED_INV_VIEW.get(stack);
        Set<FluidKey> set = inv.getStoredFluids();
        if (set.isEmpty()) {
            return FluidKeys.EMPTY;
        }
        return set.iterator().next();
    }
}
