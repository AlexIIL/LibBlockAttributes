package alexiil.mc.lib.attributes.fluid.filter;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

/** An {@link IFluidFilter} that only matches on a single {@link FluidKey}. */
public final class ExactFluidFilter implements IReadableFluidFilter {

    public final FluidKey fluid;

    public ExactFluidFilter(FluidKey fluid) {
        this.fluid = fluid;
    }

    @Override
    public boolean matches(FluidKey other) {
        return fluid.equals(other);
    }
}
