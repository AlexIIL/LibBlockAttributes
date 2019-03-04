package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidVolume;
import alexiil.mc.lib.attributes.fluid.IFluidInsertable;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;

/** An {@link IFluidInsertable} that always refuses to accept any inserted {@link FluidVolume}. */
public enum RejectingFluidInsertable implements IFluidInsertable {
    NULL,
    EXTRACTOR;

    @Override
    public FluidVolume attemptInsertion(FluidVolume stack, Simulation simulation) {
        return stack;
    }

    @Override
    public IFluidFilter getInsertionFilter() {
        return ConstantFluidFilter.NOTHING;
    }
}
