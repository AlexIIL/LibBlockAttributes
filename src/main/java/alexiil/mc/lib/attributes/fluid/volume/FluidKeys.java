/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeAccessType;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.mixin.impl.FlowableFluidAccessor;
import alexiil.mc.lib.attributes.fluid.volume.FluidEntry.FluidFloatingEntry;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey.FluidKeyBuilder;

/** The central registry for storing {@link FluidKey} instances, and mapping {@link Fluid} and {@link Potion} instances
 * to them. */
public final class FluidKeys {
    private FluidKeys() {}

    public static final Identifier MISSING_SPRITE = new Identifier("minecraft", "missingno");

    private static final String ERROR_CAST_MIXIN_JUNIT_SUFFIX
        = "cannot be cast to alexiil.mc.lib.attributes.fluid.mixin.impl.FlowableFluidAccessor";

    public static final FluidKey EMPTY;
    public static final FluidKey LAVA;
    public static final BiomeSourcedFluidKey WATER;

    private static final Map<Fluid, FluidKey> FLUIDS = new IdentityHashMap<>();
    private static final Map<Potion, FluidKey> POTIONS = new IdentityHashMap<>();
    private static final Map<FluidRegistryEntry<?>, FluidKey> OTHERS = new HashMap<>();
    private static final Map<FluidFloatingEntry, FluidKey> FLOATING = new HashMap<>();

    /*
     * Synchronisation notes:
     *
     * In theory this should use a ReadWriteLock to allow all of the
     * readers to read without blocking each other. However in practice
     * there's only going to be 2 threads accessing this (client and
     * server) so there's probably no point.
     *
     * (In addition the only operations that is performed is HashMap.get
     *  anyway, so we would probably only loose performance).  
     */

    static {
        // Empty doesn't have a proper sprite or text component because it doesn't ever make sense to use it.
        EMPTY = new SimpleFluidKey(
            new FluidKeyBuilder(Fluids.EMPTY)//
                .setName(new LiteralText("!EMPTY FLUID!"))
        );
        LAVA = createImplicitFluid(Fluids.LAVA);
        ((MutableText) LAVA.name).setStyle(Style.EMPTY.withColor(Formatting.RED));

        WATER = WaterFluidKey.INSTANCE;
        ((MutableText) WATER.name).setStyle(Style.EMPTY.withColor(Formatting.BLUE));

        put(Fluids.EMPTY, EMPTY);
        put(Fluids.LAVA, LAVA);
        put(Fluids.WATER, WATER);
        put(Potions.EMPTY, EMPTY);
        put(Potions.WATER, WATER);
    }

    public static synchronized void put(Fluid fluid, FluidKey fluidKey) {
        FLUIDS.put(fluid, fluidKey);
        if (fluid instanceof FlowableFluid) {
            FLUIDS.put(((FlowableFluid) fluid).getStill(), fluidKey);
            FLUIDS.put(((FlowableFluid) fluid).getFlowing(), fluidKey);
        }
    }

    public static synchronized void put(Potion potion, FluidKey fluidKey) {
        POTIONS.put(potion, fluidKey);
    }

    public static synchronized void put(FluidRegistryEntry<?> entry, FluidKey fluidKey) {
        if (entry.backingRegistry == Registry.FLUID) {
            put((Fluid) entry.backingObject, fluidKey);
        } else if (entry.backingObject == Registry.POTION) {
            put((Potion) entry.backingObject, fluidKey);
        } else {
            OTHERS.put(entry, fluidKey);
        }
    }

    public static synchronized void put(FluidFloatingEntry entry, FluidKey fluidKey) {
        FLOATING.put(entry, fluidKey);
    }

    /** Removes a fluid entry from this map.
     * 
     * @deprecated Because I think fluids are meant to be all statically created? */
    @Deprecated
    public static synchronized void remove(Fluid fluid) {
        FLUIDS.remove(fluid);
    }

    /** @return Null if the passed fluid is null, or a non-null {@link FluidKey}. */
    public static synchronized FluidKey get(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        FluidKey fluidKey = FLUIDS.get(fluid);
        if (fluidKey == null) {
            if (fluid instanceof FlowableFluid) {
                FlowableFluid base = (FlowableFluid) fluid;
                fluid = base.getStill();
                if (fluid == null) {
                    throw new IllegalStateException("fluid.getStill() returned a null fluid! (from " + fluid + ")");
                }
            }
            fluidKey = createImplicitFluid(fluid);
            put(fluid, fluidKey);
        }
        return fluidKey;
    }

    private static SimpleFluidKey createImplicitFluid(Fluid fluid) {
        Block block = fluid.getDefaultState().getBlockState().getBlock();
        Text name = new TranslatableText(block.getTranslationKey());
        FluidKeyBuilder builder = new FluidKeyBuilder(fluid).setName(name);
        if (fluid instanceof FlowableFluid) {
            FlowableFluid bf = (FlowableFluid) fluid;
            try {
                builder.setViscosity(FluidAmount.of(bf.getTickRate(VoidWorldView.OVERWORLD), 5));
                builder.setNetherViscosity(FluidAmount.of(bf.getTickRate(VoidWorldView.NETHER), 5));
                FlowableFluidAccessor bfa = (FlowableFluidAccessor) bf;
                builder.setCohesion(FluidAmount.ofWhole(bfa.lba_getFlowSpeed(VoidWorldView.OVERWORLD)));
                builder.setNetherCohesion(FluidAmount.ofWhole(bfa.lba_getFlowSpeed(VoidWorldView.NETHER)));
            } catch (UnsupportedOperationException uoe) {
                if (!uoe.getMessage().startsWith(VoidWorldView.ERROR_MESSAGE_START)) {
                    throw uoe;
                }
            } catch (ClassCastException cce) {
                // Mixin's don't load when using junit
                if (cce.getMessage().endsWith(ERROR_CAST_MIXIN_JUNIT_SUFFIX)) {
                    try {
                        Class.forName("org.junit.Assert");
                    } catch (ClassNotFoundException e) {
                        cce.addSuppressed(new Error("Not running a test!", e));
                        throw cce;
                    }
                }
            }
        }
        return new SimpleFluidKey(builder);
    }

    public static synchronized FluidKey get(Potion potion) {
        FluidKey fluidKey = POTIONS.get(potion);
        if (fluidKey == null) {
            fluidKey = new PotionFluidKey(potion);
            put(potion, fluidKey);
        }
        return fluidKey;
    }

    @Nullable
    public static synchronized FluidKey get(FluidEntry entry) {
        if (entry instanceof FluidFloatingEntry) {
            return FLOATING.get(entry);
        }
        if (entry instanceof FluidRegistryEntry<?>) {
            FluidRegistryEntry<?> re = (FluidRegistryEntry<?>) entry;
            // Potions are created "on demand" rather than all upfront
            // so we hack around that by adding potions here.
            if (re.backingRegistry == Registry.POTION) {
                Potion potion = (Potion) re.backingObject;
                return get(potion);
            }
            // custom, simple, modded fluids are also created "on demand"
            if (re.backingRegistry == Registry.FLUID) {
                Fluid fluid = (Fluid) re.backingObject;
                return get(fluid);
            }
            return OTHERS.get(re);
        }
        throw new IllegalArgumentException("Unknown FluidEntry " + entry.getClass());
    }

    /** @return A copy of all the {@link FluidRegistryEntry}s registered. */
    public static synchronized Set<FluidRegistryEntry<?>> getRegistryFluidIds() {
        return new HashSet<>(OTHERS.keySet());
    }

    /** @return A copy of all the {@link FluidFloatingEntry}s registered. */
    public static synchronized Set<FluidFloatingEntry> getFloatingFluidIds() {
        return new HashSet<>(FLOATING.keySet());
    }

    /** Private helper for determining the viscosity and cohesion of fluids. */
    private enum VoidWorldView implements WorldView {
        OVERWORLD(false),
        NETHER(true);

        static final String ERROR_MESSAGE_START = "LBA_Unsupported_";

        private final BiomeAccess biomeAccess;
        private final DimensionType dim;

        private VoidWorldView(boolean nether) {
            Biome biome = nether ? Biomes.NETHER_WASTES : Biomes.PLAINS;
            BiomeAccessType biomeType = (a, b, c, d, e) -> biome;
            biomeAccess = new BiomeAccess((x, y, z) -> biome, 42, biomeType);
            dim = RegistryTracker.create().get(Registry.DIMENSION_TYPE_KEY).get()
                .get(nether ? DimensionType.THE_NETHER_REGISTRY_KEY : DimensionType.OVERWORLD_REGISTRY_KEY);
        }

        @Override
        public String toString() {
            return "LBA_VoidWorldView_" + name();
        }

        @Override
        public LightingProvider getLightingProvider() {
            return null;
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return Blocks.AIR.getDefaultState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.getDefaultState();
        }

        @Override
        public WorldBorder getWorldBorder() {
            throw new UnsupportedOperationException(ERROR_MESSAGE_START + "getWorldBorder");
        }

        @Override
        public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
            throw new UnsupportedOperationException(ERROR_MESSAGE_START + "getChunk");
        }

        @Override
        public boolean isChunkLoaded(int chunkX, int chunkZ) {
            return false;
        }

        @Override
        public int getTopY(Type heightmap, int x, int z) {
            return 1;
        }

        @Override
        public int getAmbientDarkness() {
            return 0;
        }

        @Override
        public BiomeAccess getBiomeAccess() {
            return biomeAccess;
        }

        @Override
        public Biome getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
            return Biomes.DEFAULT;
        }

        @Override
        public boolean isClient() {
            return true;
        }

        @Override
        public int getSeaLevel() {
            return 0;
        }

        @Override
        public DimensionType getDimension() {
            return dim;
        }

        @Override
        public float getBrightness(Direction direction, boolean shaded) {
            return 1;
        }

        @Override
        public Stream<VoxelShape> getEntityCollisions(Entity entity, Box box, Predicate<Entity> predicate) {
            return Stream.empty();
        }
    }
}
