package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.fluid.IFluidExtractable;
import alexiil.mc.lib.attributes.fluid.IFluidInsertable;
import alexiil.mc.lib.attributes.fluid.IFluidInvStats;

/** A {@link SimpleFixedFluidInv} with a few optimisations to make the {@link IFluidInsertable},
 * {@link IFluidExtractable}, and {@link IFluidInvStats} implementations much faster than a simple inventory search for
 * larger inventories. */
public class JumboFixedFluidInv extends SimpleFixedFluidInv {

    public JumboFixedFluidInv(int invSize, int tankCapacity) {
        super(invSize, tankCapacity);
    }

    // TODO: Optimisations!
}
