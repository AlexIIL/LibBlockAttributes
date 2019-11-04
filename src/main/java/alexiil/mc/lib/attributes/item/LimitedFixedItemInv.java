/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import javax.annotation.Nullable;

import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.impl.DelegatingFixedItemInv;

/** A type of {@link FixedItemInv} that wraps an existing {@link FixedItemInv} and provides custom control over the
 * actual modification of the wrapped {@link FixedItemInv}.
 * <p>
 * Note that it is never possible to get the wrapped {@link FixedItemInv} from this class. */
public interface LimitedFixedItemInv extends FixedItemInv {

    /** Marks this object as final, and disallows any further changes to this. If this is called then
     * {@link #asUnmodifiable()} will return this object.
     * 
     * @return this. */
    LimitedFixedItemInv markFinal();

    /** Creates a copy of this {@link LimitedFixedItemInv} (with the same backing inventory and the same rules). If this
     * has been marked as final ({@link #markFinal()}) then the copy will be marked as final as well.. */
    LimitedFixedItemInv copy();

    /** Completely clears all rules currently imposed.
     * 
     * @return This. */
    default LimitedFixedItemInv reset() {
        getAllRule().reset();
        return this;
    }

    /** @return A new {@link FixedItemInv} with the current rules of this, but that cannot be modified. */
    default FixedItemInv asUnmodifiable() {
        return DelegatingFixedItemInv.createDelegate(this);
    }

    /** @return A rule for the single slot. */
    ItemSlotLimitRule getRule(int slot);

    /** @param from The first slot (inclusive)
     * @param to The last slot (exclusive).
     * @return A rule that applies to every slot between from and to. */
    ItemSlotLimitRule getSubRule(int from, int to);

    /** @return A rule that applies to every slot in this inventory. */
    default ItemSlotLimitRule getAllRule() {
        return getSubRule(0, getSlotCount());
    }

    /** A rule for a set of slots. */
    public interface ItemSlotLimitRule {

        /** Clears all limitations for this current rule. */
        default ItemSlotLimitRule reset() {
            return allowExtraction().allowInsertion().noExtractionFilter().noInsertionFilter();
        }

        /** Completely disallows extraction of items.
         * 
         * @return this. */
        default ItemSlotLimitRule disallowExtraction() {
            return setMinimum(64);
        }

        /** Stops disallowing extraction of items.
         * 
         * @return this. */
        default ItemSlotLimitRule allowExtraction() {
            return setMinimum(0);
        }

        /** Filters all insertions with the given filter in addition to whatever filters are already present.
         * 
         * @param filter Null or {@link ConstantItemFilter#ANYTHING} to clear the current filter. */
        ItemSlotLimitRule filterInserts(@Nullable ItemFilter filter);

        /** Filters all extractions with the given filter in addition to whatever filters are already present.
         * 
         * @param filter Null or {@link ConstantItemFilter#ANYTHING} to clear the current filter. */
        ItemSlotLimitRule filterExtracts(@Nullable ItemFilter filter);

        /** Removes the current {@link #filterInserts(ItemFilter)}.
         * 
         * @deprecated Because this is ambiguous as the name could refer to the count, or the filter, so instead you
         *             should use {@link #noInsertionFilter()}. */
        @Deprecated
        default ItemSlotLimitRule noInsertionLimits() {
            return filterInserts(null);
        }

        /** Removes the current {@link #filterInserts(ItemFilter) insertion filter}. */
        default ItemSlotLimitRule noInsertionFilter() {
            return filterInserts(null);
        }

        /** Removes the current {@link #filterExtracts(ItemFilter) extraction filter}. */
        default ItemSlotLimitRule noExtractionFilter() {
            return filterExtracts(null);
        }

        /** Completely disallows inserting items by setting the {@link #limitInsertionCount(int)} to 0. (This leaves the
         * {@link #filterInserts(ItemFilter)} alone, however setting it to {@link ConstantItemFilter#NOTHING} would have
         * the same effect).
         * 
         * @return this. */
        default ItemSlotLimitRule disallowInsertion() {
            return limitInsertionCount(0);
        }

        /** Stops disallowing insertion of items by setting the {@link #limitInsertionCount(int)} to 64.
         * 
         * @return this. */
        default ItemSlotLimitRule allowInsertion() {
            return limitInsertionCount(64);
        }

        /** Limits the number of items that can be inserted (in total) to the given count.
         * 
         * @param max The maximum, clamped between 0 and 64. (A value of 0 disallows insertion, and a value of 64
         *            removes the limit).
         * @return this. */
        ItemSlotLimitRule limitInsertionCount(int max);

        /** Limits the number of items that can be extracted to ensure that this slot cannot have an amount that goes
         * below the given value. (This of course has no effect on the underlying inventory, so it is always possible
         * for the underlying inventory to be modified to contain less than the given amount).
         * 
         * @param min The minimum, clamped between 0 and 64. (A value of 0 removes this limit, and a value of 64
         *            disallows extraction).
         * @return this. */
        ItemSlotLimitRule setMinimum(int min);
    }
}
