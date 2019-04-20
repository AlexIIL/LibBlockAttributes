package alexiil.mc.lib.attributes.fluid;

import java.util.function.Function;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** A delegating accessor of a single slot in a {@link FixedFluidInv}. */
public final class SingleFluidTank extends SingleFluidTankView {

    SingleFluidTank(FixedFluidInv backingView, int tank) {
        super(backingView, tank);
    }

    @Override
    public final FixedFluidInv getBackingInv() {
        return (FixedFluidInv) this.backingView;
    }

    public final boolean set(FluidVolume to, Simulation simulation) {
        return getBackingInv().setInvFluid(tank, to, simulation);
    }

    /** Sets the stack in the given slot to the given stack, or throws an exception if it was not permitted. */
    public final void forceSet(FluidVolume to) {
        getBackingInv().forceSetInvFluid(tank, to);
    }

    /** Applies the given function to the stack held in the slot, and uses {@link #forceSet(FluidVolume)} on the
     * result (Which will throw an exception if the returned stack is not valid for this inventory). */
    public final void modify(Function<FluidVolume, FluidVolume> function) {
        getBackingInv().modifyTank(tank, function);
    }
}
