/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat.mod.emi.iteminv;

import alexiil.mc.lib.attributes.Attribute;
import alexiil.mc.lib.attributes.AttributeSourceType;
import alexiil.mc.lib.attributes.item.ItemAttributes;

import dev.emi.iteminventory.api.ItemInventory;

public final class EmiItemInvCompat {
    private EmiItemInvCompat() {}

    static void load() {
        put(ItemAttributes.INSERTABLE);
        put(ItemAttributes.EXTRACTABLE);
        put(ItemAttributes.GROUPED_INV_VIEW);
        put(ItemAttributes.GROUPED_INV);
        put(ItemAttributes.FIXED_INV_VIEW);
        put(ItemAttributes.FIXED_INV);
    }

    private static void put(Attribute<?> attribute) {
        AttributeSourceType srcType = AttributeSourceType.COMPAT_WRAPPER;
        attribute.putItemClassAdder(srcType, ItemInventory.class, true, (stack, excess, to) -> {
            to.offer(new FixedInvEmiItemInv(stack, excess));
        });
    }
}
