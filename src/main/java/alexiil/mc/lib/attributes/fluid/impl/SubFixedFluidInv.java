package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidVolume;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInv;

public class SubFixedFluidInv<InvType extends IFixedFluidInv> extends SubFixedFluidInvView<InvType>
    implements IFixedFluidInv {

    public SubFixedFluidInv(InvType inv, int fromIndex, int toIndex) {
        super(inv, fromIndex, toIndex);
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        return inv.setInvFluid(getInternalTank(tank), to, simulation);
    }
}
