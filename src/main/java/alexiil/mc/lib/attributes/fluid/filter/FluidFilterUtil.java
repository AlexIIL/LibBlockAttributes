package alexiil.mc.lib.attributes.fluid.filter;

public enum FluidFilterUtil {
    ;

    public static boolean hasIntersection(FluidFilter a, FluidFilter b) {
        FluidFilter combined = a.and(b);
        return combined != ConstantFluidFilter.NOTHING;
    }
}
