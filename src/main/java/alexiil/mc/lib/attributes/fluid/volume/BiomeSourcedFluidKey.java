package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class BiomeSourcedFluidKey extends NormalFluidKey {
    public BiomeSourcedFluidKey(Fluid fluid) {
        super(fluid);
    }

    public BiomeSourcedFluidKey(Fluid fluid, Identifier spriteId) {
        super(fluid, spriteId);
    }

    public BiomeSourcedFluidKey(Fluid fluid, Identifier spriteId, int renderColor) {
        super(fluid, spriteId, renderColor);
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
