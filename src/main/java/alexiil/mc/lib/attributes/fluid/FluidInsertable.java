package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Defines an object that can have fluids inserted into it. */
public interface FluidInsertable {

    /** Inserts the given stack into this insertable, and returns the excess.
     * 
     * @param fluid The incoming fluid. Must not be modified by this call.
     * @param simulation If {@link Simulation#SIMULATE} then this shouldn't modify anything.
     * @return the excess {@link FluidVolume} that wasn't accepted. This will be independent of this insertable, however
     *         it might be the given stack instead of a completely new object. */
    FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation);

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
}
