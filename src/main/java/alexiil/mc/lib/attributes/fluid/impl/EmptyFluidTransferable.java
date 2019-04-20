package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidExtractable;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.FluidTransferable;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** An {@link FluidTransferable} that never returns any items from
 * {@link #attemptExtraction(FluidFilter, int, Simulation)}, nor accepts any items in
 * {@link #attemptInsertion(FluidVolume, Simulation)}. */
public enum EmptyFluidTransferable implements FluidTransferable {
    /** An {@link FluidTransferable} that should be treated as equal to null in all circumstances - that is any checks
     * that depend on an object being transferable should be considered FALSE for this instance. */
    NULL,

    /** An {@link FluidTransferable} that informs callers that it will interact with a nearby {@link FluidTransferable},
     * {@link FluidExtractable}, or {@link FluidInsertable} but doesn't expose any other item based attributes. */
    CONTROLLER;

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        return fluid;
    }

    @Override
    public FluidFilter getInsertionFilter() {
        return ConstantFluidFilter.NOTHING;
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
        return FluidKeys.EMPTY.withAmount(0);
    }
}
