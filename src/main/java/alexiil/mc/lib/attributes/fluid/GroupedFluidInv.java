package alexiil.mc.lib.attributes.fluid;

/** A modifiable version of {@link GroupedFluidInvView}, except that all modification methods are provided by
 * {@link FluidExtractable} and {@link FluidInsertable}. */
public interface GroupedFluidInv extends GroupedFluidInvView, FluidTransferable {
    // Nothing to declare here as all of the relevant modification methods are already declared in ItemTransferable
}
