package alexiil.mc.lib.attributes.fluid.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import alexiil.mc.lib.attributes.fluid.FluidKey;
import alexiil.mc.lib.attributes.fluid.IFluidInvStats;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;

public class CombinedFluidInvStats implements IFluidInvStats {

    private final List<? extends IFluidInvStats> statistics;

    public CombinedFluidInvStats(List<? extends IFluidInvStats> statistics) {
        this.statistics = statistics;
    }

    @Override
    public FluidInvStatistic getStatistics(IFluidFilter filter) {
        int amount = 0;
        int spaceAddable = 0;
        int spaceTotal = 0;
        for (IFluidInvStats stats : statistics) {
            FluidInvStatistic stat = stats.getStatistics(filter);
            amount += stat.amount;
            spaceAddable += stat.spaceAddable;
            spaceTotal += stat.spaceTotal;
        }
        return new FluidInvStatistic(filter, amount, spaceAddable, spaceTotal);
    }

    @Override
    public Set<FluidKey> getStoredFluids() {
        Set<FluidKey> set = new HashSet<>();
        for (IFluidInvStats stats : statistics) {
            set.addAll(stats.getStoredFluids());
        }
        return set;
    }
}
