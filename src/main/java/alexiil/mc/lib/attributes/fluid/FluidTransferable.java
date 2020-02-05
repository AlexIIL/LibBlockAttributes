/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.FilteredFluidTransferable;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Combined interface for {@link FluidInsertable} and {@link FluidExtractable}. (This is provided for simplicity when
 * exposing inventories for modification but when you don't want to implement the full set of methods that
 * {@link GroupedFluidInv} provides). */
public interface FluidTransferable extends FluidInsertable, FluidExtractable {

    /** @return A new {@link FluidTransferable} that has an additional filter applied to the fluid extracted from it or
     *         inserted into it. */
    @Override
    default FluidTransferable filtered(FluidFilter filter) {
        return new FilteredFluidTransferable(this, filter);
    }

    /** @return A new {@link FluidTransferable} that will insert into the given insertable, and never return any Fluids
     *         from {@link #attemptExtraction(FluidFilter, int, Simulation)}. */
    @Nonnull
    public static FluidTransferable from(FluidInsertable insertable) {
        return new FluidTransferable() {

            @Override
            public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
                return insertable.attemptInsertion(fluid, simulation);
            }

            @Override
            public FluidFilter getInsertionFilter() {
                return insertable.getInsertionFilter();
            }

            @Override
            @Nullable
            public FluidAmount getMinimumAcceptedAmount() {
                return insertable.getMinimumAcceptedAmount();
            }

            @Override
            public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
                return FluidKeys.EMPTY.withAmount(FluidAmount.ZERO);
            }

            @Override
            public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
                return FluidKeys.EMPTY.withAmount(FluidAmount.ZERO);
            }
        };
    }

    /** @return A new {@link FluidTransferable} that will extract from the given extractable, and reject every inserted
     *         stack. */
    @Nonnull
    public static FluidTransferable from(FluidExtractable extractable) {
        return new FluidTransferable() {

            @Override
            public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
                return fluid;
            }

            @Override
            public FluidFilter getInsertionFilter() {
                return ConstantFluidFilter.NOTHING;
            }

            @Override
            @Deprecated
            public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
                return extractable.attemptExtraction(filter, maxAmount, simulation);
            }

            @Override
            @Deprecated
            public FluidVolume attemptAnyExtraction(int maxAmount, Simulation simulation) {
                return extractable.attemptAnyExtraction(maxAmount, simulation);
            }

            @Override
            public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
                return extractable.attemptExtraction(filter, maxAmount, simulation);
            }

            @Override
            public FluidVolume attemptAnyExtraction(FluidAmount maxAmount, Simulation simulation) {
                return extractable.attemptAnyExtraction(maxAmount, simulation);
            }
        };
    }

    /** @return A new {@link FluidTransferable} that will extract from the given extractable, and insert into the given
     *         extractable. */
    @Nonnull
    public static FluidTransferable from(FluidInsertable insertable, FluidExtractable extractable) {
        if (insertable == extractable && insertable instanceof FluidTransferable) {
            return (FluidTransferable) insertable;
        }
        return new FluidTransferable() {
            @Override
            public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
                return insertable.attemptInsertion(fluid, simulation);
            }

            @Override
            public FluidFilter getInsertionFilter() {
                return insertable.getInsertionFilter();
            }

            @Override
            @Nullable
            public FluidAmount getMinimumAcceptedAmount() {
                return insertable.getMinimumAcceptedAmount();
            }

            @Override
            @Deprecated
            public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
                return extractable.attemptExtraction(filter, maxAmount, simulation);
            }

            @Override
            @Deprecated
            public FluidVolume attemptAnyExtraction(int maxAmount, Simulation simulation) {
                return extractable.attemptAnyExtraction(maxAmount, simulation);
            }

            @Override
            public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
                return extractable.attemptExtraction(filter, maxAmount, simulation);
            }

            @Override
            public FluidVolume attemptAnyExtraction(FluidAmount maxAmount, Simulation simulation) {
                return extractable.attemptAnyExtraction(maxAmount, simulation);
            }
        };
    }
}
