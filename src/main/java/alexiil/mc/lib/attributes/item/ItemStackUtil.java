/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import java.util.Objects;

import net.minecraft.item.ItemStack;

public final class ItemStackUtil {
    private ItemStackUtil() {}

    /** Checks to see if the two {@link ItemStack}'s are equal, but ignoring the {@link ItemStack#getCount() counts}. */
    public static boolean areEqualIgnoreAmounts(ItemStack a, ItemStack b) {
        if (a.isEmpty()) {
            return b.isEmpty();
        }
        if (b.isEmpty()) {
            return false;
        }
        return a.getItem() == b.getItem() && Objects.equals(a.getNbt(), b.getNbt());
    }
}
