package alexiil.mc.lib.attributes.fluid;

import java.util.function.Function;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.GroupedFluidInvFixedWrapper;
import alexiil.mc.lib.attributes.fluid.impl.MappedFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.SubFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** A changeable {@link FixedFluidInvView} that can have it's contents changed. Note that this does not imply that the
 * contents can be changed to anything the caller wishes them to be.
 * <p>
 * The attribute is stored in {@link FluidAttributes#FIXED_INV}.
 * <p>
 */
public interface FixedFluidInv extends FixedFluidInvView {

    /** Sets the fluid in the given tank to the given fluid.
     * 
     * @return True if the modification was allowed, false otherwise. (For example if the given stack doesn't pass the
     *         {@link FixedFluidInvView#isFluidValidForTank(int, FluidKey)} test). */
    boolean setInvFluid(int tank, FluidVolume to, Simulation simulation);

    /** Sets the stack in the given slot to the given stack, or throws an exception if it was not permitted. */
    default void forceSetInvFluid(int slot, FluidVolume to) {
        if (!setInvFluid(slot, to, Simulation.ACTION)) {
            throw new IllegalStateException("Unable to force-set the tank " + slot + " to " + to + "!");
        }
    }

    /** Applies the given function to the stack held in the slot, and uses {@link #forceSetInvFluid(int, FluidVolume)}
     * on the result (Which will throw an exception if the returned stack is not valid for this inventory). */
    default void modifyTank(int tank, Function<FluidVolume, FluidVolume> function) {
        forceSetInvFluid(tank, function.apply(getInvFluid(tank)));
    }

    @Override
    default SingleFluidTank getTank(int tank) {
        return new SingleFluidTank(this, tank);
    }

    /** @return An {@link FluidInsertable} for this inventory that will attempt to insert into any of the tanks in this
     *         inventory. */
    default FluidInsertable getInsertable() {
        return getGroupedInv();
    }

    /** @return An {@link FluidExtractable} for this inventory that will attempt to extract from any of the tanks in
     *         this inventory. */
    default FluidExtractable getExtractable() {
        return getGroupedInv();
    }

    /** @return An {@link FluidTransferable} for this inventory that will attempt to extract from any of the tanks in
     *         this inventory. */
    default FluidTransferable getTransferable() {
        return getGroupedInv();
    }

    @Override
    default GroupedFluidInv getGroupedInv() {
        return new GroupedFluidInvFixedWrapper(this);
    }

    @Override
    default FixedFluidInv getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedFluidInv.INSTANCE;
        }
        return new SubFixedFluidInv(this, fromIndex, toIndex);
    }

    @Override
    default FixedFluidInv getMappedInv(int... slots) {
        if (slots.length == 0) {
            return EmptyFixedFluidInv.INSTANCE;
        }
        return new MappedFixedFluidInv(this, slots);
    }
}
