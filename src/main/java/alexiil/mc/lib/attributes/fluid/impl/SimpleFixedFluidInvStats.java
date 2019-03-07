package alexiil.mc.lib.attributes.fluid.impl;

import java.util.HashSet;
import java.util.Set;

import alexiil.mc.lib.attributes.fluid.IFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.IFluidInvStats;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilterUtil;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public final class SimpleFixedFluidInvStats implements IFluidInvStats {

    private final IFixedFluidInvView inv;

    public SimpleFixedFluidInvStats(IFixedFluidInvView inv) {
        this.inv = inv;
    }

    @Override
    public FluidInvStatistic getStatistics(IFluidFilter filter) {
        int amount = 0;
        int space = 0;
        int totalSpace = 0;
        boolean totalSpaceValid = true;
        for (int t = 0; t < inv.getTankCount(); t++) {
            int max = inv.getMaxAmount(t);
            FluidVolume fluid = inv.getInvFluid(t);
            if (!fluid.isEmpty()) {
                if (filter.matches(fluid.fluidKey)) {
                    amount += fluid.getAmount();
                    space += max - fluid.getAmount();
                }
                continue;
            }
            if (FluidFilterUtil.hasIntersection(filter, inv.getFilterForTank(t))) {
                totalSpace += max;
            }
        }
        return new FluidInvStatistic(filter, amount, space, totalSpaceValid ? totalSpace : -1);
    }

    @Override
    public Set<FluidKey> getStoredFluids() {
        Set<FluidKey> set = new HashSet<>();
        for (int s = 0; s < inv.getTankCount(); s++) {
            FluidVolume fluid = inv.getInvFluid(s);
            if (!fluid.isEmpty()) {
                set.add(fluid.fluidKey);
            }
        }
        return set;
    }
}
