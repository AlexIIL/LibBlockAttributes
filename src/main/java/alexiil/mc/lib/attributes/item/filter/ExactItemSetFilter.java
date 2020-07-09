/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;

/** A {@link ReadableItemFilter}, similar to {@link ExactItemFilter} but which matches any of the {@link Item}s in a
 * {@link Set}. */
public final class ExactItemSetFilter implements ReadableItemFilter {

    private final Set<Item> items;

    public ExactItemSetFilter(Set<Item> items) {
        this.items = Collections.unmodifiableSet(items);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return items.contains(stack.getItem());
    }

    public Set<Item> getItems() {
        return items;
    }

    public static ReadableItemFilter anyOf(Collection<? extends ItemConvertible> items) {
        return ExactItemFilter.anyOf(items);
    }

    public static ReadableItemFilter anyOf(ItemConvertible[] items) {
        return ExactItemFilter.anyOf(items);
    }
}
