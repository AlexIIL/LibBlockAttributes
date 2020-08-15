/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import net.minecraft.item.Item;
import net.minecraft.tag.Tag;

/** A {@link ResolvableItemFilter} that matches any {@link Item}s in a {@link Tag}. */
public final class ItemTagFilter implements ResolvableItemFilter {

    public final Tag<Item> tag;

    public ItemTagFilter(Tag<Item> tag) {
        this.tag = tag;
    }

    @Override
    public ReadableItemFilter resolve() {
        return ExactItemFilter.anyOf(tag.values());
    }
}
