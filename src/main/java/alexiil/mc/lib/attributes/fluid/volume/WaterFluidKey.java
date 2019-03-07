package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.fluid.Fluids;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/* package-private */ final class WaterFluidKey extends BiomeSourcedFluidKey {

    private static final Identifier WATER_SPRITE_ID = new Identifier("minecraft", "blocks/water_still");

    static final WaterFluidKey INSTANCE = new WaterFluidKey();

    private WaterFluidKey() {
        super(Fluids.WATER, WATER_SPRITE_ID);
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
