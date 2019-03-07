package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.potion.Potion;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class PotionFluidKey extends FluidKey {

    private static final Identifier POTION_TEXTURE;

    static {
        // POTION_TEXTURE = new Identifier("libblockattributes", "fluids/potion")
        POTION_TEXTURE = new Identifier("minecraft", "blocks/water_still");
    }

    public final Potion potion;

    /* package-private */ PotionFluidKey(Potion potion) {
        super(new FluidRegistryEntry<>(Registry.POTION, potion), POTION_TEXTURE);
        this.potion = potion;
    }

    @Override
    public PotionFluidVolume readVolume(CompoundTag tag) {
        return new PotionFluidVolume(this, tag);
    }

    @Override
    public PotionFluidVolume withAmount(int amount) {
        return new PotionFluidVolume(this, amount);
    }
}
