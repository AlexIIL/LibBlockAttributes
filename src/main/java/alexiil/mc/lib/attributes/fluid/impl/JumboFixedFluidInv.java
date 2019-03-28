package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.fluid.FluidExtractable;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.FluidInvStats;

/** A {@link SimpleFixedFluidInv} with a few optimisations to make the {@link FluidInsertable},
 * {@link FluidExtractable}, and {@link FluidInvStats} implementations much faster than a simple inventory search for
 * larger inventories. */
public class JumboFixedFluidInv extends SimpleFixedFluidInv {

    public JumboFixedFluidInv(int invSize, int tankCapacity) {
        super(invSize, tankCapacity);
    }

    // TODO: Optimisations!
}
