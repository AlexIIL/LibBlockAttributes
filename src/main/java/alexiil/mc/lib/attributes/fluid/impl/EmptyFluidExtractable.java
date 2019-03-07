package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.IFluidExtractable;
import alexiil.mc.lib.attributes.fluid.IFluidInsertable;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** An {@link IFluidExtractable} that never returns any items from
 * {@link #attemptExtraction(IFluidFilter, int, Simulation)}. */
public enum EmptyFluidExtractable implements IFluidExtractable {
    /** An {@link IFluidExtractable} that should be treated as equal to null in all circumstances - that is any checks
     * that depend on an object being extractable should be considered FALSE for this instance. */
    NULL,

    /** An {@link IFluidExtractable} that informs callers that it will push items into a nearby
     * {@link IFluidInsertable}, but doesn't expose any other item based attributes.
     * <p>
     * The buildcraft quarry is a good example of this - it doesn't have any inventory tanks itself and it pushes items
     * out of it as it mines them from the world, but item pipes should still connect to it so that it can insert into
     * them. */
    SUPPLIER;

    @Override
    public FluidVolume attemptExtraction(IFluidFilter filter, int maxCount, Simulation simulation) {
        return FluidKeys.EMPTY.withAmount(0);
    }
}
