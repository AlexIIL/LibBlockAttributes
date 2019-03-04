package alexiil.mc.lib.attributes.fluid.filter;

public enum FluidFilterUtil {
    ;

    public static boolean hasIntersection(IFluidFilter a, IFluidFilter b) {
        IFluidFilter combined = a.and(b);
        return combined != ConstantFluidFilter.NOTHING;
    }
}
