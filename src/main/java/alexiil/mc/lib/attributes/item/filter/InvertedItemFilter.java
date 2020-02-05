/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import net.minecraft.item.ItemStack;

public final class InvertedItemFilter implements ReadableItemFilter {

    public final ItemFilter delegate;

    public InvertedItemFilter(ItemFilter delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return !delegate.matches(stack);
    }

    @Override
    public ItemFilter negate() {
        return delegate;
    }
}
