package alexiil.mc.lib.attributes.fluid.impl;

import java.util.List;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** An {@link IFixedFluidInv} that delegates to a list of them instead of storing items directly. */
public class CombinedFixedFluidInv<InvType extends IFixedFluidInv> extends CombinedFixedFluidInvView<InvType>
    implements IFixedFluidInv {

    public CombinedFixedFluidInv(List<? extends InvType> views) {
        super(views);
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        return getInv(tank).setInvFluid(getSubTank(tank), to, simulation);
    }
}
