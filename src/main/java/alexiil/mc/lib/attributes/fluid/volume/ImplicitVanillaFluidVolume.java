package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;

final class ImplicitVanillaFluidVolume extends NormalFluidVolume {

    ImplicitVanillaFluidVolume(ImplicitVanillaFluidKey fluid, int amount) {
        super(fluid, amount);
    }

    ImplicitVanillaFluidVolume(ImplicitVanillaFluidKey fluid, CompoundTag tag) {
        super(fluid, tag);
    }
}
