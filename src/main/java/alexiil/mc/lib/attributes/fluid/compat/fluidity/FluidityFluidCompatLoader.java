package alexiil.mc.lib.attributes.fluid.compat.fluidity;

import net.fabricmc.loader.api.FabricLoader;

public final class FluidityFluidCompatLoader {
    private FluidityFluidCompatLoader() {}

    public static void load() {
        if (FabricLoader.getInstance().isModLoaded("fluidity")) {
            FluidityFluidCompat.load();   
        }
    }
}
