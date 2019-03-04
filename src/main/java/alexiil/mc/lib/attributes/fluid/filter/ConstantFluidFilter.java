package alexiil.mc.lib.attributes.fluid.filter;

import alexiil.mc.lib.attributes.fluid.FluidKey;

public enum ConstantFluidFilter implements IReadableFluidFilter {
    ANYTHING(true),
    NOTHING(false);

    private final boolean result;

    private ConstantFluidFilter(boolean result) {
        this.result = result;
    }

    public static ConstantFluidFilter of(boolean result) {
        return result ? ANYTHING : NOTHING;
    }

    @Override
    public boolean matches(FluidKey fluid) {
        if (fluid.isEmpty()) {
            throw new IllegalArgumentException("You should never test an IFluidFilter with an empty fluid!");
        }
        return result;
    }

    @Override
    public IFluidFilter negate() {
        return of(!result);
    }

    @Override
    public IFluidFilter and(IFluidFilter other) {
        if (result) {
            return other;
        } else {
            return NOTHING;
        }
    }

    @Override
    public IFluidFilter or(IFluidFilter other) {
        if (result) {
            return ANYTHING;
        } else {
            return other;
        }
    }

    // Don't override asPredicate so that we still get the better version that calls our own negate(), and(), or()
    // methods.
}
