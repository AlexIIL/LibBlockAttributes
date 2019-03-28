package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;

import alexiil.mc.lib.attributes.fluid.render.FluidVolumeRenderer;
import alexiil.mc.lib.attributes.fluid.render.ImplicitVanillaFluidVolumeRenderer;

final class ImplicitVanillaFluidVolume extends NormalFluidVolume {

    ImplicitVanillaFluidVolume(ImplicitVanillaFluidKey fluid, int amount) {
        super(fluid, amount);
    }

    ImplicitVanillaFluidVolume(ImplicitVanillaFluidKey fluid, CompoundTag tag) {
        super(fluid, tag);
    }

    @Override
    public FluidVolumeRenderer getRenderer() {
        return ImplicitVanillaFluidVolumeRenderer.INSTANCE;
    }
}
