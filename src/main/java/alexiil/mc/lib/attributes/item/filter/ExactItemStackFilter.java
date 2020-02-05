/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.ItemStackUtil;

/** An {@link ItemFilter} that only matches on a single {@link ItemStack}, using
 * {@link ItemStackUtil#areEqualIgnoreAmounts(ItemStack, ItemStack)}. */
public final class ExactItemStackFilter implements ReadableItemFilter {

    public final ItemStack stack;

    public ExactItemStackFilter(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public boolean matches(ItemStack other) {
        return ItemStackUtil.areEqualIgnoreAmounts(this.stack, other);
    }
}
