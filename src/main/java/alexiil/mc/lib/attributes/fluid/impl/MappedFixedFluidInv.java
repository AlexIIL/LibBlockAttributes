package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class MappedFixedFluidInv extends MappedFixedFluidInvView implements FixedFluidInv {

    public MappedFixedFluidInv(FixedFluidInv inv, int[] tanks) {
        super(inv, tanks);
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        return ((FixedFluidInv) inv).setInvFluid(getInternalTank(tank), to, simulation);
    }
}
