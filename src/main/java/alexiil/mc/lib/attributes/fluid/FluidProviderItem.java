/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.Ref;

/** An interface for {@link Item}'s to implement if they can be filled and drained like a bucket. */
public interface FluidProviderItem {
    /* NOTE: As I haven't attempted to make attributes for items yet this is all that there is. */

    /** Attempts to drain some fluid from the given stack.
     * 
     * @param stack A reference to the input stack - the stack itself should never be modified, but the reference should
     *            be.
     * @return The drained fluid, or {@link FluidVolume#isEmpty()} if nothing was drained. */
    FluidVolume drain(Ref<ItemStack> stack);

    /** Attempts to fill the given stack with the given fluid.
     * 
     * @param stack A reference to the input stack - the stack itself should never be modified, but the reference should
     *            be.
     * @param with A reference to the input fluid - the stack itself should never be modified, but the reference should
     *            be.
     * @return True if the stack was drained, false otherwise */
    boolean fill(Ref<ItemStack> stack, Ref<FluidVolume> with);
}
