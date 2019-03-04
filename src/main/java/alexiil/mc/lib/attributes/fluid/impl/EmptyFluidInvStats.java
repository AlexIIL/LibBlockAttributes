package alexiil.mc.lib.attributes.fluid.impl;

import java.util.Collections;
import java.util.Set;

import alexiil.mc.lib.attributes.fluid.FluidKey;
import alexiil.mc.lib.attributes.fluid.IFluidInvStats;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;

/** {@link IFluidInvStats} for an empty inventory. */
public enum EmptyFluidInvStats implements IFluidInvStats {
    INSTANCE;

    @Override
    public FluidInvStatistic getStatistics(IFluidFilter filter) {
        return new FluidInvStatistic(filter, 0, 0, 0);
    }

    @Override
    public Set<FluidKey> getStoredFluids() {
        return Collections.emptySet();
    }
}
