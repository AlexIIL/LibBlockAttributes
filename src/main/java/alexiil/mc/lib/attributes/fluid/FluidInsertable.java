/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Defines an object that can have fluids inserted into it. */
@FunctionalInterface
public interface FluidInsertable {

    /** Inserts the given stack into this insertable, and returns the excess.
     * 
     * @param fluid The incoming fluid. Must not be modified by this call.
     * @param simulation If {@link Simulation#SIMULATE} then this shouldn't modify anything.
     * @return the excess {@link FluidVolume} that wasn't accepted. This will be independent of this insertable, however
     *         it might be the given object instead of a completely new object. */
    FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation);

    /** Inserts the given stack into this insertable, and returns the excess.
     * <p>
     * This is equivalent to calling {@link #attemptInsertion(FluidVolume, Simulation)} with a {@link Simulation}
     * parameter of {@link Simulation#ACTION ACTION}.
     * 
     * @param fluid The incoming fluid. Must not be modified by this call.
     * @return the excess {@link FluidVolume} that wasn't accepted. This will be independent of this insertable, however
     *         it might be the given stack instead of a completely new object. */
    default FluidVolume insert(FluidVolume fluid) {
        return attemptInsertion(fluid, Simulation.ACTION);
    }

    /** @return The minimum amount of fluid that {@link #attemptInsertion(FluidVolume, Simulation)} will actually
     *         accept. Note that this only provides a guarantee that {@link FluidVolume fluid volumes} with an
     *         {@link FluidVolume#getAmount() amount} less than this will never be accepted. */
    default int getMinimumAcceptedAmount() {
        return 1;
    }

    /** Returns an {@link FluidFilter} to determine if {@link #attemptInsertion(FluidVolume, Simulation)} will accept a
     * stack. The default implementation is a call to {@link #attemptInsertion(FluidVolume, Simulation)
     * attemptInsertion}(stack, {@link Simulation#SIMULATE}), and it is only useful to override this if the resulting
     * filter contains information that might be usable by the caller.
     * 
     * @return A filter to determine if {@link #attemptInsertion(FluidVolume, Simulation)} will accept the entirety of a
     *         given stack. */
    default FluidFilter getInsertionFilter() {
        return fluid -> {
            FluidVolume volume = fluid.withAmount(Integer.MAX_VALUE);
            return attemptInsertion(volume, Simulation.SIMULATE).getAmount() < Integer.MAX_VALUE;
        };
    }

    /** @return An object that only implements {@link FluidInsertable}, and does not expose any of the other
     *         modification methods that sibling or subclasses offer (like {@link FluidExtractable} or
     *         {@link GroupedFluidInv}. */
    default FluidInsertable getPureInsertable() {
        final FluidInsertable delegate = this;
        return new FluidInsertable() {
            @Override
            public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
                return delegate.attemptInsertion(fluid, simulation);
            }

            @Override
            public FluidFilter getInsertionFilter() {
                return delegate.getInsertionFilter();
            }
        };
    }
}
