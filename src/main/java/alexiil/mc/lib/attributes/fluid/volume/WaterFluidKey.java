package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/* package-private */ final class WaterFluidKey extends BiomeSourcedFluidKey {

    static final WaterFluidKey INSTANCE = new WaterFluidKey();

    private WaterFluidKey() {
        super(NormalFluidKey.builder(Fluids.WATER, //
            new Identifier("minecraft", "block/water_still"), //
            new TranslatableComponent("block.minecraft.water")//
        ));
    }

    @Override
    public BiomeSourcedFluidVolume readVolume(CompoundTag tag) {
        return new WaterFluidVolume(tag);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(int amount) {
        return new WaterFluidVolume(amount);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(Biome source, int amount) {
        return new WaterFluidVolume(source, amount);
    }
}
