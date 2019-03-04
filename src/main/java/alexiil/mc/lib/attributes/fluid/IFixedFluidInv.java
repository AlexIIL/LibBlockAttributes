package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFluidExtractable;
import alexiil.mc.lib.attributes.fluid.impl.RejectingFluidInsertable;
import alexiil.mc.lib.attributes.fluid.impl.SimpleFixedFluidInvExtractable;
import alexiil.mc.lib.attributes.fluid.impl.SimpleFixedFluidInvInsertable;
import alexiil.mc.lib.attributes.fluid.impl.SubFixedFluidInv;

public interface IFixedFluidInv extends IFixedFluidInvView {

    /** Sets the fluid in the given tank to the given fluid.
     * 
     * @return True if the modification was allowed, false otherwise. (For example if the given stack doesn't pass the
     *         {@link IFixedFluidInvView#isFluidValidForTank(int, FluidKey)} test). */
    boolean setInvFluid(int tank, FluidVolume to, Simulation simulation);

    /** @return An {@link IFluidInsertable} for this inventory that will attempt to insert into any of the tanks in this
     *         inventory. */
    default IFluidInsertable getInsertable() {
        return new SimpleFixedFluidInvInsertable(this, null);
    }

    /** @return An {@link IFluidInsertable} for this inventory that will attempt to insert into only the given array of
     *         tanks. */
    default IFluidInsertable getInsertable(int[] tanks) {
        if (tanks.length == 0) {
            return RejectingFluidInsertable.NULL;
        }
        return new SimpleFixedFluidInvInsertable(this, tanks);
    }

    /** @return An {@link IFluidExtractable} for this inventory that will attempt to extract from any of the tanks in
     *         this inventory. */
    default IFluidExtractable getExtractable() {
        return new SimpleFixedFluidInvExtractable(this, null);
    }

    /** @return An {@link IFluidExtractable} for this inventory that will attempt to extract from only the given array
     *         of tanks. */
    default IFluidExtractable getExtractable(int[] tanks) {
        if (tanks.length == 0) {
            return EmptyFluidExtractable.NULL;
        }
        return new SimpleFixedFluidInvExtractable(this, tanks);
    }

    @Override
    default IFixedFluidInv getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedFluidInv.INSTANCE;
        }
        return new SubFixedFluidInv<>(this, fromIndex, toIndex);
    }
}
