/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import net.minecraft.item.ItemStack;

/** A {@link ReadableItemFilter} that only matches items that are {@link Class#isInstance(Object)} of a certain
 * {@link Class}, and are NOT {@link ItemStack#isEmpty() empty} . */
public final class ItemClassFilter implements ReadableItemFilter {

    public final Class<?> matchedClass;

    public ItemClassFilter(Class<?> matchedClass) {
        this.matchedClass = matchedClass;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && matchedClass.isInstance(stack.getItem());
    }
}
