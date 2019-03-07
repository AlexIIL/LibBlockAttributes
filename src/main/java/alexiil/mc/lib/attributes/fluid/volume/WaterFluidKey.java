package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.fluid.Fluids;
import net.minecraft.text.TranslatableTextComponent;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/* package-private */ final class WaterFluidKey extends BiomeSourcedFluidKey {

    static final WaterFluidKey INSTANCE = new WaterFluidKey();

    private WaterFluidKey() {
        super(new NormalFluidKeyBuilder(Fluids.WATER)//
            .setSpriteId(new Identifier("minecraft", "block/water_still"))//
            .setTextComponent(new TranslatableTextComponent("block.minecraft.water")));
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
