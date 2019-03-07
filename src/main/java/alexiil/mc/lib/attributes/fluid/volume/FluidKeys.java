package alexiil.mc.lib.attributes.fluid.volume;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import alexiil.mc.lib.attributes.fluid.volume.NormalFluidKey.NormalFluidKeyBuilder;
import alexiil.mc.lib.attributes.fluid.volume.NormalFluidKey.SimpleFluidKey;

public class FluidKeys {

    public static final NormalFluidKey EMPTY;
    public static final NormalFluidKey LAVA;
    public static final BiomeSourcedFluidKey WATER;

    private static final Map<FluidRegistryEntry<?>, FluidKey> MASTER_MAP = new HashMap<>();

    private static final Map<Fluid, FluidKey> FLUIDS = new IdentityHashMap<>();
    private static final Map<Potion, FluidKey> POTIONS = new IdentityHashMap<>();

    static {
        EMPTY = new SimpleFluidKey(new NormalFluidKeyBuilder(Fluids.EMPTY));
        LAVA = new SimpleFluidKey(new NormalFluidKeyBuilder(Fluids.LAVA)//
            .setSpriteId(new Identifier("minecraft", "block/lava_still")));
        WATER = WaterFluidKey.INSTANCE;

        put(Fluids.EMPTY, EMPTY);
        put(Fluids.LAVA, LAVA);
        put(Fluids.WATER, WATER);
        put(Fluids.FLOWING_LAVA, LAVA);
        put(Fluids.FLOWING_WATER, WATER);
        put(Potions.EMPTY, EMPTY);
        put(Potions.WATER, WATER);
    }

    public static void put(Fluid fluid, FluidKey fluidKey) {
        FLUIDS.put(fluid, fluidKey);
        MASTER_MAP.put(fluidKey.registryEntry, fluidKey);
    }

    public static void put(Potion potion, FluidKey fluidKey) {
        POTIONS.put(potion, fluidKey);
        MASTER_MAP.put(fluidKey.registryEntry, fluidKey);
    }

    /** Removes a fluid entry from this map.
     * 
     * @deprecated Because I think fluids are meant to be all statically created? */
    @Deprecated
    public static void remove(Fluid fluid) {
        FLUIDS.remove(fluid);
    }

    @Nullable
    public static FluidKey get(@Nullable Fluid fluid) {
        return FLUIDS.get(fluid);
    }

    public static FluidKey get(Potion potion) {
        FluidKey fluidKey = POTIONS.get(potion);
        if (fluidKey == null) {
            fluidKey = new PotionFluidKey(potion);
            POTIONS.put(potion, fluidKey);
            MASTER_MAP.put(fluidKey.registryEntry, fluidKey);
        }
        return fluidKey;
    }

    @Nullable
    static FluidKey get(FluidRegistryEntry<?> entry) {
        FluidKey fluidKey = MASTER_MAP.get(entry);
        // Potions are created "on demand" rather than all upfront
        // so we hack around that by adding potions here.
        if (fluidKey == null && entry.backingRegistry == Registry.POTION) {
            Potion potion = (Potion) entry.backingObject;
            return get(potion);
        }
        return fluidKey;
    }
}
