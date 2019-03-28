package alexiil.mc.lib.attributes.fluid.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import alexiil.mc.lib.attributes.fluid.FluidInvStats;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

public class CombinedFluidInvStats implements FluidInvStats {

    private final List<? extends FluidInvStats> statistics;

    public CombinedFluidInvStats(List<? extends FluidInvStats> statistics) {
        this.statistics = statistics;
    }

    @Override
    public FluidInvStatistic getStatistics(FluidFilter filter) {
        int amount = 0;
        int spaceAddable = 0;
        int spaceTotal = 0;
        for (FluidInvStats stats : statistics) {
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
        for (FluidInvStats stats : statistics) {
            set.addAll(stats.getStoredFluids());
        }
        return set;
    }
}
