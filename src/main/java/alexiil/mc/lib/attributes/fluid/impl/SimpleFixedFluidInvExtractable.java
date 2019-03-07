package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.IFluidExtractable;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public final class SimpleFixedFluidInvExtractable implements IFluidExtractable {

    private final IFixedFluidInv inv;

    /** Null means that this can extract from any of the tanks. */
    private final int[] tanks;

    public SimpleFixedFluidInvExtractable(IFixedFluidInv inv, int[] tanks) {
        this.inv = inv;
        this.tanks = tanks;
    }

    @Override
    public FluidVolume attemptExtraction(IFluidFilter filter, int maxCount, Simulation simulation) {

        FluidVolume fluid = FluidKeys.EMPTY.withAmount(0);
        if (tanks == null) {
            for (int t = 0; t < inv.getTankCount(); t++) {
                FluidVolume invFluid = inv.getInvFluid(t);
                if (invFluid.isEmpty() || !filter.matches(invFluid.fluidKey)) {
                    continue;
                }
                invFluid = invFluid.copy();
                FluidVolume addable = invFluid.split(maxCount);
                FluidVolume merged = FluidVolume.merge(fluid, addable);
                if (merged != null && inv.setInvFluid(t, invFluid, simulation)) {
                    maxCount -= addable.getAmount();
                    fluid = merged;
                    assert maxCount >= 0;
                    if (maxCount <= 0) {
                        return fluid;
                    }
                }
            }
        } else {
            for (int t : tanks) {
                // Copy of above
                FluidVolume invFluid = inv.getInvFluid(t);
                if (invFluid.isEmpty() || !filter.matches(invFluid.fluidKey)) {
                    continue;
                }
                invFluid = invFluid.copy();
                FluidVolume addable = invFluid.split(maxCount);
                FluidVolume merged = FluidVolume.merge(fluid, addable);
                if (merged != null && inv.setInvFluid(t, invFluid, simulation)) {
                    maxCount -= addable.getAmount();
                    fluid = merged;
                    assert maxCount >= 0;
                    if (maxCount <= 0) {
                        return fluid;
                    }
                }
            }
        }

        return fluid;
    }
}
