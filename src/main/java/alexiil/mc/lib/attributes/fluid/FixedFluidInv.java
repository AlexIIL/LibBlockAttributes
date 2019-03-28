package alexiil.mc.lib.attributes.fluid;

import net.minecraft.util.shape.VoxelShape;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.CacheInfo;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFluidExtractable;
import alexiil.mc.lib.attributes.fluid.impl.RejectingFluidInsertable;
import alexiil.mc.lib.attributes.fluid.impl.SimpleFixedFluidInvExtractable;
import alexiil.mc.lib.attributes.fluid.impl.SimpleFixedFluidInvInsertable;
import alexiil.mc.lib.attributes.fluid.impl.SubFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public interface FixedFluidInv extends FixedFluidInvView {

    /** Sets the fluid in the given tank to the given fluid.
     * 
     * @return True if the modification was allowed, false otherwise. (For example if the given stack doesn't pass the
     *         {@link FixedFluidInvView#isFluidValidForTank(int, FluidKey)} test). */
    boolean setInvFluid(int tank, FluidVolume to, Simulation simulation);

    /** @return An {@link FluidInsertable} for this inventory that will attempt to insert into any of the tanks in this
     *         inventory. */
    default FluidInsertable getInsertable() {
        return new SimpleFixedFluidInvInsertable(this, null);
    }

    /** @return An {@link FluidInsertable} for this inventory that will attempt to insert into only the given array of
     *         tanks. */
    default FluidInsertable getInsertable(int[] tanks) {
        if (tanks.length == 0) {
            return RejectingFluidInsertable.NULL;
        }
        return new SimpleFixedFluidInvInsertable(this, tanks);
    }

    /** @return An {@link FluidExtractable} for this inventory that will attempt to extract from any of the tanks in
     *         this inventory. */
    default FluidExtractable getExtractable() {
        return new SimpleFixedFluidInvExtractable(this, null);
    }

    /** @return An {@link FluidExtractable} for this inventory that will attempt to extract from only the given array of
     *         tanks. */
    default FluidExtractable getExtractable(int[] tanks) {
        if (tanks.length == 0) {
            return EmptyFluidExtractable.NULL;
        }
        return new SimpleFixedFluidInvExtractable(this, tanks);
    }

    @Override
    default FixedFluidInv getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedFluidInv.INSTANCE;
        }
        return new SubFixedFluidInv<>(this, fromIndex, toIndex);
    }

    @Override
    default void offerSelfAsAttribute(AttributeList<?> list, CacheInfo cacheInfo, VoxelShape shape) {
        FixedFluidInvView.super.offerSelfAsAttribute(list, cacheInfo, shape);
        list.offer(getInsertable(), cacheInfo, shape);
        list.offer(getExtractable(), cacheInfo, shape);
    }
}
