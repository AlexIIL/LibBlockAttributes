package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.biome.Biome;

public class BiomeSourcedFluidKey extends NormalFluidKey {
    public BiomeSourcedFluidKey(NormalFluidKeyBuilder builder) {
        super(builder);
    }

    @Override
    public BiomeSourcedFluidVolume readVolume(CompoundTag tag) {
        return new BiomeSourcedFluidVolume(this, tag);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        BiomeSourcedFluidKey other = (BiomeSourcedFluidKey) obj;
        return fluid == other.fluid;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(fluid);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(int amount) {
        return new BiomeSourcedFluidVolume(this, amount);
    }

    public BiomeSourcedFluidVolume withAmount(Biome source, int amount) {
        return new BiomeSourcedFluidVolume(this, source, amount);
    }
}
