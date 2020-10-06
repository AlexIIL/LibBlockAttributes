/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidExtractable;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.NullVariant;

/** A {@link FluidExtractable} that never returns any fluid from
 * {@link #attemptExtraction(FluidFilter, int, Simulation)}. */
public enum EmptyFluidExtractable implements FluidExtractable, NullVariant {
    /** A {@link FluidExtractable} that should be treated as equal to null in all circumstances - that is any checks
     * that depend on an object being extractable should be considered FALSE for this instance. */
    NULL,

    /** A {@link FluidExtractable} that informs callers that it will push fluid into a nearby {@link FluidInsertable},
     * but doesn't expose any other fluid based attributes.
     * <p>
     * The buildcraft quarry is a good example of an item version of this - it doesn't have any inventory storage itself
     * and it pushes items out of it as it mines them from the world, but item pipes should still connect to it so that
     * it can insert into them. */
    SUPPLIER;

    @Override
    @Deprecated // (since = "0.6.0", forRemoval = true)
    public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
        return FluidKeys.EMPTY.withAmount(FluidAmount.ZERO);
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        return FluidKeys.EMPTY.withAmount(FluidAmount.ZERO);
    }

    @Override
    public FluidExtractable getPureExtractable() {
        return this;
    }
}
