package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

/* package-private */ final class WaterFluidVolume extends BiomeSourcedFluidVolume {

    public WaterFluidVolume(int amount) {
        super(WaterFluidKey.INSTANCE, amount);
    }

    public WaterFluidVolume(Biome source, int amount) {
        super(WaterFluidKey.INSTANCE, source, amount);
    }

    @Override
    public int getRenderColor() {
        Object2IntMap<Biome> sources = getBiomeSources();
        int biomeCount = sources.size();
        switch (biomeCount) {
            case 0: {
                // Um, what?
                return Biomes.DEFAULT.getWaterColor();
            }
            case 1: {
                return sources.keySet().iterator().next().getWaterColor();
            }
            default: {
                int r = 0;
                int g = 0;
                int b = 0;
                int total = 0;

                for (Biome biome : sources.keySet()) {
                    int amount = sources.getInt(biome);
                    int colour = biome.getWaterColor();
                    r += colour * amount;
                    g += (colour >> 8) * amount;
                    b += (colour >> 16) * amount;
                    total += amount;
                }

                r /= total;
                g /= total;
                b /= total;

                assert r >= 0;
                assert g >= 0;
                assert b >= 0;

                assert r < 256;
                assert g < 256;
                assert b < 256;

                // Opposite way round because biomes are apparently the wrong way round for opengl
                return (r << 16) | (g << 8) | (b);
            }
        }
    }
}
