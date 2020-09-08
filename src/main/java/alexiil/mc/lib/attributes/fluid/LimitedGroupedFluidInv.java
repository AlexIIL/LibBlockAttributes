/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.DelegatingGroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

public interface LimitedGroupedFluidInv extends GroupedFluidInv {

    /** Marks this object as final, and disallows any further changes to this. If this is called then
     * {@link #asUnmodifiable()} will return this object.
     * 
     * @return this. */
    LimitedGroupedFluidInv markFinal();

    /** Creates a copy of this {@link LimitedGroupedFluidInv} (with the same backing inventory and the same rules), but
     * not marked as final. */
    LimitedGroupedFluidInv copy();

    /** Completely clears all rules currently imposed.
     * 
     * @return This. */
    default LimitedGroupedFluidInv reset() {
        getAllRule().reset();
        return this;
    }

    /** @return A new {@link GroupedFluidInv} with the current rules of this, but that cannot be modified. */
    default GroupedFluidInv asUnmodifiable() {
        return new DelegatingGroupedFluidInv(this);
    }

    /** @return An {@link FluidLimitRule} that applies to everything. */
    default FluidLimitRule getAllRule() {
        return getRule(ConstantFluidFilter.ANYTHING);
    }

    /** @return A rule for the specific {@link FluidKey} given. */
    default FluidLimitRule getRule(FluidKey key) {
        return getRule(key.exactFilter);
    }

    /** @return An {@link FluidLimitRule} that applies to every fluid that matches the given filter. */
    FluidLimitRule getRule(FluidFilter filter);

    /** A rule that is applied once for every {@link FluidKey} that matches the {@link FluidFilter} that was used in
     * {@link LimitedGroupedFluidInv#getRule(FluidFilter)}. */
    public interface FluidLimitRule {

        /** Completely disallows insertion and extraction for this {@link FluidLimitRule}. */
        default FluidLimitRule disallowTransfer() {
            return disallowInsertion().disallowExtraction();
        }

        /** Clears all limitations for this current rule. */
        default FluidLimitRule reset() {
            return allowExtraction().allowInsertion();
        }

        /** Disallows insertion for this {@link FluidLimitRule}. */
        default FluidLimitRule disallowInsertion() {
            return limitInsertionAmount(FluidAmount.ZERO);
        }

        /** Resets any insertion limitations previously imposed by this {@link FluidLimitRule}. */
        default FluidLimitRule allowInsertion() {
            return limitInsertionAmount(FluidAmount.NEGATIVE_ONE);
        }

        /** Limits the amount of fluid that can be inserted (in total) to the given amount.
         * 
         * @param max The maximum. A value less than 0 will reset this back to no limits.
         * @return this.
         * @deprecated Replaced by {@link #limitInsertionAmount(FluidAmount)} */
        @Deprecated
        default FluidLimitRule limitInsertionCount(int max) {
            return limitInsertionAmount(FluidAmount.of1620(max));
        }

        /** Limits the amount of fluid that can be inserted (in total) to the given amount.
         * 
         * @param max The maximum. A value less than 0 will reset this back to no limits.
         * @return this. */
        FluidLimitRule limitInsertionAmount(FluidAmount max);

        /** Completely disallows extraction of fluids.
         * 
         * @return this. */
        default FluidLimitRule disallowExtraction() {
            return setMinimum(FluidAmount.MAX_BUCKETS);
        }

        /** Stops disallowing extraction of fluids.
         * 
         * @return this. */
        default FluidLimitRule allowExtraction() {
            return setMinimum(FluidAmount.ZERO);
        }

        /** Limits the amount of fluid that can be extracted to ensure that the inventory cannot have an amount below
         * the given value. (This of course has no effect on the underlying inventory, so it is always possible for the
         * underlying inventory to be modified to contain less than the given amount).
         * 
         * @param min The minimum number of items. A value of 0 removes the rule for this {@link FluidFilter}.
         * @return this.
         * @deprecated Replaced by {@link #setMinimum(FluidAmount)}. */
        @Deprecated
        default FluidLimitRule setMinimum(int min) {
            return setMinimum(FluidAmount.of1620(min));
        }

        /** Limits the amount of fluid that can be extracted to ensure that the inventory cannot have an amount below
         * the given value. (This of course has no effect on the underlying inventory, so it is always possible for the
         * underlying inventory to be modified to contain less than the given amount).
         * 
         * @param min The minimum amount of fluid. A value of 0 removes the rule for this {@link FluidFilter}.
         * @return this. */
        FluidLimitRule setMinimum(FluidAmount min);
    }
}
