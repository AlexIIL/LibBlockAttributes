package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class SubFixedFluidInv extends SubFixedFluidInvView implements FixedFluidInv {

    public SubFixedFluidInv(FixedFluidInv inv, int fromIndex, int toIndex) {
        super(inv, fromIndex, toIndex);
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        return ((FixedFluidInv) inv).setInvFluid(getInternalTank(tank), to, simulation);
    }
}
