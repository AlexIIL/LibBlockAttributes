/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** An {@link ItemFilter} that only matches on a single {@link Item}. */
public final class ExactItemFilter implements ReadableItemFilter {

    public final Item item;

    public ExactItemFilter(Item item) {
        this.item = item;
    }

    /** @return Either {@link ConstantItemFilter#NOTHING} if {@link ItemConvertible#asItem()} returns {@link Items#AIR},
     *         or an {@link ExactItemFilter} if it returns any other {@link Item}. */
    public static ReadableItemFilter createFilter(ItemConvertible entry) {
        Item item = convert(entry);
        if (item == null) {
            return ConstantItemFilter.NOTHING;
        }
        return new ExactItemFilter(item);
    }

    public static ReadableItemFilter anyOf(Collection<? extends ItemConvertible> items) {
        return anyOf(items.toArray(new ItemConvertible[0]));
    }

    public static ReadableItemFilter anyOf(ItemConvertible[] items) {
        if (items.length == 0) {
            return ConstantItemFilter.NOTHING;
        } else if (items.length == 1) {
            return createFilter(items[0]);
        } else {
            Set<Item> set = new HashSet<>();
            for (ItemConvertible cvt : items) {
                Item item = convert(cvt);
                if (item != null) {
                    set.add(item);
                }
            }
            switch (set.size()) {
                case 0:
                    return ConstantItemFilter.NOTHING;
                case 1:
                    return new ExactItemFilter(set.iterator().next());
                default:
                    return new ExactItemSetFilter(set);
            }
        }
    }

    @Nullable
    static Item convert(ItemConvertible from) {
        Item item = from.asItem();
        if (item == Items.AIR) {
            return null;
        }
        return item;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.getItem() == item;
    }
}
