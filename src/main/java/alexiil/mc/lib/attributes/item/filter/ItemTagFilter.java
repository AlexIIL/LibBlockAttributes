/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;

/** A {@link ResolvableItemFilter} that matches any {@link Item}s in a {@link TagKey}. */
public final class ItemTagFilter implements ResolvableItemFilter {

    public final TagKey<Item> tag;

    public ItemTagFilter(TagKey<Item> tag) {
        this.tag = tag;
    }

    @Override
    public ReadableItemFilter resolve() {
        List<Item> items = new ArrayList<>();
        for (RegistryEntry<Item> entry : Registries.ITEM.iterateEntries(tag)) {
            items.add(entry.value());
        }
        return ExactItemFilter.anyOf(items);
    }
}
