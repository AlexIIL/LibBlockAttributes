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

import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.Ref;

/** An interface for {@link Item}'s to implement if they can be filled and drained like a bucket.
 * 
 * @deprecated This has been completely replaced by item-based attribute accessors and
 *             {@link FluidContainerRegistry}. */
@Deprecated(since = "0.5.0", forRemoval = true)
public interface FluidProviderItem {
    /** Attempts to drain some fluid from the given stack.
     * 
     * @param stack A reference to the input stack - the stack itself should never be modified, but the reference should
     *            be.
     * @return The drained fluid, or {@link FluidVolume#isEmpty()} if nothing was drained.
     * @deprecated This has been replaced by {@link FluidAttributes#EXTRACTABLE}.
     *             {@link CombinableAttribute#get(alexiil.mc.lib.attributes.misc.Reference) get}(stack).
     *             {@link FluidExtractable#extract(int) extract(maximumAmount)} */
    @Deprecated(since = "0.5.0", forRemoval = true)
    FluidVolume drain(Ref<ItemStack> stack);

    /** Attempts to fill the given stack with the given fluid.
     * 
     * @param stack A reference to the input stack - the stack itself should never be modified, but the reference should
     *            be.
     * @param with A reference to the input fluid - the stack itself should never be modified, but the reference should
     *            be.
     * @return True if the stack was drained, false otherwise
     * @deprecated This has been replaced by {@link FluidAttributes#INSERTABLE}.
     *             {@link CombinableAttribute#get(alexiil.mc.lib.attributes.misc.Reference) get}(stack).
     *             {@link FluidInsertable#insert(FluidVolume) insert}(with) */
    @Deprecated(since = "0.5.0", forRemoval = true)
    boolean fill(Ref<ItemStack> stack, Ref<FluidVolume> with);
}
