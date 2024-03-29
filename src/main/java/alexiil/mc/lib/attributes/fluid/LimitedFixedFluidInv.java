/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import javax.annotation.Nullable;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.DelegatingFixedFluidInv;
import alexiil.mc.lib.attributes.item.LimitedFixedItemInv;

/** A type of {@link FixedFluidInv} that wraps an existing {@link FixedFluidInv} and provides custom control over the
 * actual modification of the wrapped {@link FixedFluidInv}.
 * <p>
 * Note that it is never possible to get the wrapped {@link FixedFluidInv} from this class. */
public interface LimitedFixedFluidInv extends FixedFluidInv {
    /** Marks this object as final, and disallows any further changes to this. If this is called then
     * {@link #asUnmodifiable()} will return this object.
     * 
     * @return this. */
    LimitedFixedFluidInv markFinal();

    /** Creates a copy of this {@link LimitedFixedItemInv} (with the same backing inventory and the same rules). */
    LimitedFixedFluidInv copy();

    /** Completely clears all rules currently imposed.
     * 
     * @return This. */
    default LimitedFixedFluidInv reset() {
        getAllRule().reset();
        return this;
    }

    /** @return A new {@link FixedFluidInv} with the current rules of this, but that cannot be modified. */
    default FixedFluidInv asUnmodifiable() {
        return new DelegatingFixedFluidInv(this);
    }

    /** @return A rule for the single tank. */
    FluidTankLimitRule getRule(int tank);

    /** @param from The first tank (inclusive)
     * @param to The last tank (exclusive).
     * @return A rule that applies to every tank between from and to. */
    FluidTankLimitRule getSubRule(int from, int to);

    /** @return A rule that applies to every tank in this inventory. */
    default FluidTankLimitRule getAllRule() {
        return getSubRule(0, getTankCount());
    }

    /** A rule for a set of tanks. */
    public interface FluidTankLimitRule {

        /** Clears all limitations for this current rule. */
        default FluidTankLimitRule reset() {
            return allowExtraction().noInsertionLimits().setMinimum(0).limitInsertionCount(-1);
        }

        /** Completely disallows extraction of fluids.
         * 
         * @return this. */
        default FluidTankLimitRule disallowExtraction() {
            return setMinimum(FluidAmount.MAX_BUCKETS);
        }

        /** Stops disallowing extraction of fluids.
         * 
         * @return this. */
        default FluidTankLimitRule allowExtraction() {
            return setMinimum(null);
        }

        /** Filters all insertions with the given filter in addition to whatever filters are already present.
         * 
         * @param filter Null or {@link ConstantFluidFilter#ANYTHING} to clear the current filter. */
        FluidTankLimitRule filterInserts(@Nullable FluidFilter filter);

        /** Removes the current {@link #filterInserts(FluidFilter)}.
         * 
         * @deprecated Because this is ambiguous as the name could refer to the count, or the filter, so instead you
         *             should use {@link #noInsertionFilter()} */
        @Deprecated(since = "0.8.2", forRemoval = true)
        default FluidTankLimitRule noInsertionLimits() {
            return filterInserts(null);
        }

        /** Removes the current {@link #filterInserts(FluidFilter) insertion filter}. */
        default FluidTankLimitRule noInsertionFilter() {
            return filterInserts(null);
        }

        /** Completely disallows inserting items.
         * 
         * @return this. */
        default FluidTankLimitRule disallowInsertion() {
            return filterInserts(ConstantFluidFilter.NOTHING);
        }

        // Unlike all of the other migrations we don't add validate methods and
        // make both methods default to calling each other because the only
        // implementation ATM (that I'm aware of) is the standard, default one.

        /** Limits the amount of fluid that can be inserted (in total) to the given count.
         * 
         * @param max The maximum. A value below 0 will reset this rule.
         * @return this.
         * @deprecated Replaced by {@link #limitInsertionAmount(FluidAmount)}. */
        @Deprecated(since = "0.6.0", forRemoval = true)
        default FluidTankLimitRule limitInsertionCount(int max) {
            return limitInsertionAmount(FluidAmount.of1620(max));
        }

        /** Limits the amount of fluid that can be inserted (in total) to the given count.
         * 
         * @param max The maximum. A value below 0 will reset this rule. Null is treated as zero.
         * @return this. */
        FluidTankLimitRule limitInsertionAmount(@Nullable FluidAmount max);

        /** Limits the amount of fluid that can be extracted to ensure that this tank cannot have an amount that goes
         * below the given value. (This of course has no effect on the underlying inventory, so it is always possible
         * for the underlying inventory to be modified to contain less than the given amount).
         * 
         * @return this.
         * @deprecated Replaced by {@link #setMinimum(FluidAmount)} */
        @Deprecated(since = "0.6.0", forRemoval = true)
        default FluidTankLimitRule setMinimum(int min) {
            return setMinimum(FluidAmount.of1620(min));
        }

        /** Limits the amount of fluid that can be extracted to ensure that this tank cannot have an amount that goes
         * below the given value. (This of course has no effect on the underlying tank, so it is always possible for the
         * underlying tank to be modified to contain less than the given amount).
         * 
         * @param min The minimum. Both null and non-positive values are treated as removing the limit.
         * @return this. */
        FluidTankLimitRule setMinimum(@Nullable FluidAmount min);
    }
}
