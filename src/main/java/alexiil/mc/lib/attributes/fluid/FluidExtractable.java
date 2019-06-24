/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.item.ItemStackUtil;

/** Defines an object that can have items extracted from it. */
public interface FluidExtractable {

    /** Attempt to extract *any* {@link FluidVolume} from this that {@link FluidFilter#matches(FluidKey) matches} the
     * given {@link FluidFilter}.
     * 
     * @param filter
     * @param maxAmount The maximum amount of fluid that can be extracted. Negative numbers throw an exception.
     * @param simulation If {@link Simulation#SIMULATE} then this should return the same result that a call with
     *            {@link Simulation#ACTION} would do, but without modifying anything else.
     * @return A new, independent {@link FluidVolume} that was extracted. */
    FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation);

    /** Calls {@link #attemptExtraction(FluidFilter, int, Simulation) attemptExtraction()} with an {@link FluidFilter}
     * of {@link ConstantFluidFilter#ANYTHING}. */
    default FluidVolume attemptAnyExtraction(int maxAmount, Simulation simulation) {
        return attemptExtraction(ConstantFluidFilter.ANYTHING, maxAmount, simulation);
    }

    /** Attempt to extract *any* {@link FluidVolume} from this that {@link FluidFilter#matches(FluidKey) matches} the
     * given {@link FluidFilter}.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(FluidFilter, int, Simulation)} with a {@link Simulation}
     * parameter of {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted. */
    default FluidVolume extract(FluidFilter filter, int maxAmount) {
        return attemptExtraction(filter, maxAmount, Simulation.ACTION);
    }

    /** Attempt to extract *any* {@link ItemStack} from this that is
     * {@link ItemStackUtil#areEqualIgnoreAmounts(ItemStack, ItemStack) equal}to the given {@link ItemStack}.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(FluidFilter, int, Simulation)} with an
     * {@link FluidFilter} parameter of {@link ExactFluidFilter} and a {@link Simulation} parameter of
     * {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted. */
    default FluidVolume extract(FluidKey filter, int maxAmount) {
        return attemptExtraction(new ExactFluidFilter(filter), maxAmount, Simulation.ACTION);
    }

    /** Attempt to extract *any* {@link ItemStack} from this.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(FluidFilter, int, Simulation)} with an
     * {@link FluidFilter} parameter of {@link ConstantFluidFilter#ANYTHING} and a {@link Simulation} parameter of
     * {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted. */
    default FluidVolume extract(int maxAmount) {
        return attemptExtraction(ConstantFluidFilter.ANYTHING, maxAmount, Simulation.ACTION);
    }

    /** @return An object that only implements {@link FluidExtractable}, and does not expose any of the other
     *         modification methods that sibling or subclasses offer (like {@link FluidInsertable} or
     *         {@link GroupedFluidInv}. */
    default FluidExtractable getPureExtractable() {
        FluidExtractable delegate = this;
        return new FluidExtractable() {
            @Override
            public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
                return delegate.attemptExtraction(filter, maxAmount, simulation);
            }

            @Override
            public FluidVolume attemptAnyExtraction(int maxAmount, Simulation simulation) {
                return delegate.attemptAnyExtraction(maxAmount, simulation);
            }
        };
    }
}
