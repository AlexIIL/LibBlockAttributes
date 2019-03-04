package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidVolume;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.IFluidExtractable;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;

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

        FluidVolume fluid = new FluidVolume();
        if (tanks == null) {
            for (int t = 0; t < inv.getTankCount(); t++) {
                FluidVolume invFluid = inv.getInvFluid(t);
                if (invFluid.isEmpty() || !filter.matches(invFluid.toKey())) {
                    continue;
                }
                if (!fluid.isEmpty()) {
                    if (!FluidVolume.areEqualExceptAmounts(fluid, invFluid)) {
                        continue;
                    }
                }
                invFluid = invFluid.copy();

                FluidVolume addable = invFluid.split(maxCount);
                if (inv.setInvFluid(t, invFluid, simulation)) {

                    if (fluid.isEmpty()) {
                        fluid = addable;
                    } else {
                        fluid.add(addable.getAmount());
                    }
                    maxCount -= addable.getAmount();
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
                if (invFluid.isEmpty() || !filter.matches(invFluid.toKey())) {
                    continue;
                }
                if (!fluid.isEmpty()) {
                    if (!FluidVolume.areEqualExceptAmounts(fluid, invFluid)) {
                        continue;
                    }
                }
                invFluid = invFluid.copy();

                FluidVolume addable = invFluid.split(maxCount);
                if (inv.setInvFluid(t, invFluid, simulation)) {

                    if (fluid.isEmpty()) {
                        fluid = addable;
                    } else {
                        fluid.add(addable.getAmount());
                    }
                    maxCount -= addable.getAmount();
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
