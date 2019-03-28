package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** An {@link FluidInsertable} that always refuses to accept any inserted {@link FluidVolume}. */
public enum RejectingFluidInsertable implements FluidInsertable {
    NULL,
    EXTRACTOR;

    @Override
    public FluidVolume attemptInsertion(FluidVolume stack, Simulation simulation) {
        return stack;
    }

    @Override
    public FluidFilter getInsertionFilter() {
        return ConstantFluidFilter.NOTHING;
    }
}
