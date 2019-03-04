package alexiil.mc.lib.attributes.fluid;

public enum FluidVolumeUtil {
    ;

    private static final boolean LONG_LOCALISATION = true;
    private static final boolean USE_FULL_NAMES = true;
    private static final boolean FULLY_EXPAND = true;

    /** @param amount The amount in {@link FluidVolume#BASE_UNIT base units} */
    public static String localizeFluidAmount(int amount) {
        if (FULLY_EXPAND) {

        }
        if (LONG_LOCALISATION) {
            if (amount < FluidVolume.BASE_UNIT) {
                return "0";
            }
            // TODO: Actual localisation!
            // (I'd like to copy this almost directly from buildcraft's LocaleUtil.localizeFluid)
            if (amount < FluidVolume.NUGGET) {
                return (amount / (double) FluidVolume.NUGGET) + " Nuggets";
            }
            if (amount < FluidVolume.INGOT) {
                return (amount / (double) FluidVolume.INGOT) + " Ingots";
            }
            return (amount / (double) FluidVolume.BUCKET) + " Buckets";
        } else {
            return amount / (double) FluidVolume.BUCKET + "Buckets";
        }
    }
}
