package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;

final class ImplicitVanillaFluidKey extends NormalFluidKey {

    public ImplicitVanillaFluidKey(NormalFluidKeyBuilder builder) {
        super(builder);
    }

    @Override
    public ImplicitVanillaFluidVolume withAmount(int amount) {
        return new ImplicitVanillaFluidVolume(this, amount);
    }

    @Override
    public ImplicitVanillaFluidVolume readVolume(CompoundTag tag) {
        return new ImplicitVanillaFluidVolume(this, tag);
    }
}
