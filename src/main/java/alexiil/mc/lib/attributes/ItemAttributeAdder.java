/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;

@FunctionalInterface
public interface ItemAttributeAdder<T> {
    /* Note that we do have the type parameter (unlike AttributeProviderItem) because instances are registered to a
     * specific Attribute so it's actually useful for implementors. */

    /** Adds every attribute instance to the given list that the item itself cannot be expected to add support for.
     * 
     * @param stack A {@link Reference} to the stack to test for. If any of the added attributes need to modify the
     *            stack then they should do that by setting the given {@link Reference#set(Object)}, rather than
     *            modifying the stack directly.
     * @param excess If interacting with any of the returned attributes produces excess itemstacks then they should be
     *            added to this {@link Consumer}. */
    void addAll(Reference<ItemStack> stack, @Nullable LimitedConsumer<ItemStack> excess, ItemAttributeList<T> to);
}
