package alexiil.mc.lib.attributes.fluid.impl;

import java.util.Collections;
import java.util.Set;

import alexiil.mc.lib.attributes.fluid.FluidInvStats;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

/** {@link FluidInvStats} for an empty inventory. */
public enum EmptyFluidInvStats implements FluidInvStats {
    INSTANCE;

    @Override
    public FluidInvStatistic getStatistics(FluidFilter filter) {
        return new FluidInvStatistic(filter, 0, 0, 0);
    }

    @Override
    public Set<FluidKey> getStoredFluids() {
        return Collections.emptySet();
    }
}
