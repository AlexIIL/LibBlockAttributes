package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.potion.PotionUtil;

public final class PotionFluidVolume extends FluidVolume {

    public PotionFluidVolume(PotionFluidKey key, int amount) {
        super(key, amount);
    }

    public PotionFluidVolume(PotionFluidKey key, CompoundTag tag) {
        super(key, tag);
    }

    @Override
    public PotionFluidKey getFluidKey() {
        return (PotionFluidKey) fluidKey;
    }

    @Override
    public int getRenderColor() {
        return PotionUtil.getColor(getFluidKey().potion);
    }
}
