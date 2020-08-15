/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import com.google.common.collect.Iterators;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.AggregateFilterType;

/** An {@link ItemFilter} over a predefined array of {@link ItemFilter}'s. You can either iterate over this object
 * directly or use {@link #getFilterCount()} and {@link #getFilter(int)} to read every filter individually. */
public final class AggregateItemFilter implements ReadableItemFilter, Iterable<ItemFilter> {
    public final AggregateFilterType type;
    private final ItemFilter[] filters;

    public AggregateItemFilter(AggregateFilterType type, ItemFilter... filters) {
        if (filters.length < 2) {
            throw new IllegalArgumentException(
                "There's no reason to construct an aggregate stack filter that matches " + filters.length + " filters!"
            );
        }
        this.type = type;
        this.filters = filters;
    }

    /** @return An {@link AggregateItemFilter} that contains both of the given filters. This might not return a new
     *         object if either of the filters contains the other. */
    public static ItemFilter and(ItemFilter filterA, ItemFilter filterB) {
        return combine(AggregateFilterType.ALL, filterA, filterB);
    }

    /** @return An {@link AggregateItemFilter} that contains both of the given filters. This might not return a new
     *         object if either of the filters contains the other. */
    public static ItemFilter or(ItemFilter filterA, ItemFilter filterB) {
        return combine(AggregateFilterType.ANY, filterA, filterB);
    }

    public static ItemFilter combine(AggregateFilterType type, ItemFilter filterA, ItemFilter filterB) {
        final boolean all = type == AggregateFilterType.ALL;
        if (filterA == filterB) {
            return filterA;
        }
        if (filterA == ConstantItemFilter.ANYTHING) {
            return all ? filterB : ConstantItemFilter.ANYTHING;
        }
        if (filterB == ConstantItemFilter.ANYTHING) {
            return all ? filterA : ConstantItemFilter.ANYTHING;
        }
        if (filterA == ConstantItemFilter.NOTHING) {
            return all ? ConstantItemFilter.NOTHING : filterB;
        }
        if (filterB == ConstantItemFilter.NOTHING) {
            return all ? ConstantItemFilter.NOTHING : filterA;
        }
        if (filterA instanceof AggregateItemFilter && ((AggregateItemFilter) filterA).type == type) {
            if (filterB instanceof AggregateItemFilter && ((AggregateItemFilter) filterB).type == type) {
                ItemFilter[] filtersA = ((AggregateItemFilter) filterA).filters;
                ItemFilter[] filtersB = ((AggregateItemFilter) filterB).filters;
                ItemFilter[] filters = new ItemFilter[filtersA.length + filtersB.length];
                System.arraycopy(filtersA, 0, filters, 0, filtersA.length);
                System.arraycopy(filtersB, 0, filters, filtersA.length, filtersB.length);
                return new AggregateItemFilter(type, filters);
            }
            ItemFilter[] from = ((AggregateItemFilter) filterA).filters;
            ItemFilter[] filters = new ItemFilter[from.length + 1];
            System.arraycopy(from, 0, filters, 0, from.length);
            filters[from.length] = filterB;
            return new AggregateItemFilter(type, filters);
        }
        if (filterB instanceof AggregateItemFilter && ((AggregateItemFilter) filterB).type == type) {
            ItemFilter[] from = ((AggregateItemFilter) filterB).filters;
            ItemFilter[] filters = new ItemFilter[from.length + 1];
            System.arraycopy(from, 0, filters, 0, from.length);
            filters[from.length] = filterA;
            return new AggregateItemFilter(type, filters);
        }

        if (filterA instanceof ExactItemStackFilter) {
            ItemStack exactA = ((ExactItemStackFilter) filterA).stack;
            if (filterB.matches(exactA)) {
                return filterA;
            } else if (all) {
                return ConstantItemFilter.NOTHING;
            }
        }
        if (filterB instanceof ExactItemStackFilter) {
            ItemStack exactB = ((ExactItemStackFilter) filterB).stack;
            if (filterA.matches(exactB)) {
                return filterB;
            } else if (all) {
                return ConstantItemFilter.NOTHING;
            }
        }

        return new AggregateItemFilter(type, filterA, filterB);
    }

    public static ItemFilter allOf(ItemFilter... filters) {
        return combine(AggregateFilterType.ALL, filters);
    }

    public static ItemFilter anyOf(ItemFilter... filters) {
        return combine(AggregateFilterType.ANY, filters);
    }

    public static ItemFilter anyOf(ItemStack... stacks) {
        if (stacks == null || stacks.length == 0) {
            return ConstantItemFilter.NOTHING;
        }
        if (stacks.length == 1) {
            return new ExactItemStackFilter(stacks[0]);
        }
        ItemFilter[] filters = new ItemFilter[stacks.length];
        int i = 0;
        for (ItemStack item : stacks) {
            filters[i++] = new ExactItemStackFilter(item);
        }
        return anyOf(filters);
    }

    public static ItemFilter anyOf(Item... items) {
        if (items == null || items.length == 0) {
            return ConstantItemFilter.NOTHING;
        }
        if (items.length == 1) {
            return new ExactItemFilter(items[0]);
        }
        ItemFilter[] filters = new ItemFilter[items.length];
        int i = 0;
        for (Item item : items) {
            filters[i++] = new ExactItemFilter(item);
        }
        return anyOf(filters);
    }

    public static ItemFilter combine(AggregateFilterType type, ItemFilter... filters) {
        return combine(type, Arrays.asList(filters));
    }

    public static ItemFilter allOf(List<? extends ItemFilter> filters) {
        return combine(AggregateFilterType.ALL, filters);
    }

    public static ItemFilter anyOf(List<? extends ItemFilter> filters) {
        return combine(AggregateFilterType.ANY, filters);
    }

    public static ItemFilter combine(AggregateFilterType type, List<? extends ItemFilter> filters) {
        if (!(filters instanceof RandomAccess)) {
            filters = Arrays.asList(filters.toArray(new ItemFilter[0]));
        }
        switch (filters.size()) {
            case 0:
                return ConstantItemFilter.ANYTHING;
            case 1:
                return filters.get(0);
            case 2:
                // I'm assuming this might be faster than putting everything into a list?
                return combine(type, filters.get(0), filters.get(1));
            default: {
                ItemFilter filter = filters.get(0);
                for (int i = 1; i < filters.size(); i++) {
                    filter = combine(type, filter, filters.get(i));
                }
                return filter;
            }
        }
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (type == AggregateFilterType.ALL) {
            for (ItemFilter filter : filters) {
                if (!filter.matches(stack)) {
                    return false;
                }
            }
            return true;
        } else {
            for (ItemFilter filter : filters) {
                if (filter.matches(stack)) {
                    return true;
                }
            }
            return false;
        }
    }

    public int getFilterCount() {
        return filters.length;
    }

    public ItemFilter getFilter(int index) {
        return filters[index];
    }

    @Override
    public Iterator<ItemFilter> iterator() {
        return Iterators.forArray(filters);
    }
}
