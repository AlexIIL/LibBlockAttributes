/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.fluid.BaseFluid;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.WorldView;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.render.DefaultFluidVolumeRenderer;

/** A factory for {@link FluidVolume} instances. Identifying whether two {@link FluidKey}'s are equal is always done via
 * the object identity comparison (== rather than {@link #equals(Object)} - although {@link FluidKey} final-overrides
 * equals and hashCode to perform identity comparison anyway). */
public abstract class FluidKey {

    /** The default {@link #viscosity} that gases use: 1/24. */
    public static final FluidAmount DEFAULT_GAS_VISCOSITY = FluidAmount.of(1, 24);

    /** The default {@link #cohesion} that gases use: 1/24 */
    public static final FluidAmount DEFAULT_GAS_COHESION = FluidAmount.of(1, 24);

    /** The default {@link #density} (and {@link #thermalCapacity}) that gases use: 1/160. */
    public static final FluidAmount DEFAULT_GAS_DENSITY = FluidAmount.of(1, 160);

    private static final Identifier MISSING_SPRITE = new Identifier("minecraft", "missingno");

    /* package-private */ final FluidEntry entry;

    /** The singular (main) unit to use when displaying amounts, capacities, and flow rates to the player.
     * 
     * @deprecated Because most of the time you should use {@link #unitSet} rather than this. */
    @Deprecated
    public final FluidUnit unit;

    /** All units to use when displaying amounts, capacities, and flow rates to the player. */
    public final FluidUnitSet unitSet;

    /** Fallback for {@link DefaultFluidVolumeRenderer} to use if it can't find one itself. */
    public final Identifier spriteId, flowingSpriteId;

    /** The colour to use when rendering this {@link FluidKey}'s specifically.
     * <p>
     * Note that this might differ from the one returned by {@link FluidVolume#getRenderColor()}! */
    public final int renderColor;

    /** The name to use when displaying tooltips for this {@link FluidKey} specifically.
     * <p>
     * Note that this might differ from the one returned by {@link FluidVolume#getName()}! */
    public final Text name;

    /** True if this represents a gas, false if this represents a liquid.
     * <p>
     * it is generally recommended that a gas will have much lower {@link #density}, {@link #thermalCapacity},
     * {@link #viscosity}, and {@link #cohesion} than any liquid, however this is not technically required. */
    public final boolean gaseous;

    /** How much this fluid resists attempts to move it around. For fluid blocks this should generally be the tick-rate
     * of the block, divided by 5. (So for water this is 1, and for lava in the overworld it is 6).
     * <p>
     * It is always an error for any fluid to have a negative (or null) viscosity.
     * <p>
     * This isn't in any particular unit, and is instead relative to minecraft's water (which LBA declares as
     * {@link FluidAmount#ONE}). */
    public final FluidAmount viscosity;

    /** Special-case property for fluids to have different {@link #viscosity} values in the nether.
     * <p>
     * The same rules apply to this field as to {@link #viscosity}.
     * <p>
     * (Doing this properly would require a full-on dynamic fluid properties system that could take into account the
     * temperature of the air around it, and would add a lot of complexity before this system has had any testing, so
     * instead we'll just special-case the nether). */
    public final FluidAmount netherViscosity;

    /** How much this fluid will spread itself around. For fluid blocks this should generally be 8 divided by the block
     * spread distance. (For water this will be 1, and for lava this will be 2).
     * <p>
     * It is always an error for any fluid to have a negative (or null) cohesion.
     * <p>
     * This isn't in any particular unit, and is instead relative to minecraft's water (which LBA declares as
     * {@link FluidAmount#ONE}). */
    public final FluidAmount cohesion;

    /** Special-case property for fluids to have different {@link #cohesion} values in the nether.
     * <p>
     * The same rules apply to this field as to {@link #cohesion}.
     * <p>
     * (Doing this properly would require a full-on dynamic fluid properties system that could take into account the
     * temperature of the air around it, and would add a lot of complexity before this system has had any testing, so
     * instead we'll just special-case the nether). */
    public final FluidAmount netherCohesion;

    /** How much this fluid
     * <p>
     * This isn't in any particular unit, and is instead relative to minecraft's water (which LBA declares as
     * {@link FluidAmount#ONE}). */
    public final FluidAmount density;

    /** How much energy is required to make this fluid change it's temperature.
     * <p>
     * LBA doesn't declare any temperature scale (or constant temperature values for specific fluid keys) itself due to
     * the very different approaches that mods might wish to take to temperature.
     * <p>
     * This isn't in any particular unit, and is instead relative to minecraft's water (which LBA declares as
     * {@link FluidAmount#ONE}). */
    public final FluidAmount thermalCapacity;

    @Nullable
    private final Fluid rawFluid;

    public static class FluidKeyBuilder {
        /* package-private */ FluidEntry entry;
        /* package-private */ Identifier spriteId;
        /* package-private */ Identifier flowingSpriteId;
        /* package-private */ Text name;
        /* package-private */ int renderColor = 0xFF_FF_FF;
        /* package-private */ FluidUnit unit = FluidUnit.BUCKET;
        /* package-private */ final FluidUnitSet unitSet = new FluidUnitSet();
        /* package-private */ Fluid rawFluid;
        /* package-private */ boolean gaseous = false;
        /* package-private */ FluidAmount viscosity, netherViscosity;
        /* package-private */ FluidAmount cohesion, netherCohesion;
        /* package-private */ FluidAmount density;
        /* package-private */ FluidAmount thermalCapacity;

        /** @deprecated As the flowing sprite ID is needed as well. */
        @Deprecated
        public FluidKeyBuilder(FluidRegistryEntry<?> registryEntry, Identifier spriteId, Text name) {
            this(registryEntry, spriteId, spriteId, name);
        }

        public FluidKeyBuilder(
            FluidRegistryEntry<?> registryEntry, Identifier spriteId, Identifier flowingSpriteId, Text name
        ) {
            this.entry = registryEntry;
            this.spriteId = spriteId;
            this.flowingSpriteId = flowingSpriteId;
            this.name = name;
        }

        public FluidKeyBuilder() {}

        /** Uses {@link #setIdEntry(Identifier)} */
        public FluidKeyBuilder(Identifier id) {
            setIdEntry(id);
        }

        public FluidKeyBuilder(Fluid fluid) {
            this.entry = new FluidRegistryEntry<>(Registry.FLUID, fluid);
            this.rawFluid = fluid;
        }

        public FluidKeyBuilder copyFrom(FluidKeyBuilder from) {
            this.entry = from.entry;
            this.spriteId = from.spriteId;
            this.flowingSpriteId = from.flowingSpriteId;
            this.name = from.name;
            this.renderColor = from.renderColor;
            this.unit = from.unit;
            this.unitSet.units.clear();
            this.unitSet.copyFrom(from.unitSet);
            this.rawFluid = from.rawFluid;
            this.gaseous = from.gaseous;
            this.viscosity = from.viscosity;
            this.netherViscosity = from.netherViscosity;
            this.cohesion = from.cohesion;
            this.netherCohesion = from.netherCohesion;
            this.density = from.density;
            this.thermalCapacity = from.thermalCapacity;
            return this;
        }

        public FluidKeyBuilder setRegistryEntry(FluidRegistryEntry<?> registryEntry) {
            this.entry = registryEntry;
            return this;
        }

        /** @param id The floating identifier to use to identify this fluid - this will not be backed by a normal
         *            {@link DefaultedRegistry}, instead by the map in {@link FluidEntry#ofId(Identifier)}. */
        public FluidKeyBuilder setIdEntry(Identifier id) {
            this.entry = FluidEntry.ofId(id);
            return this;
        }

        public FluidKeyBuilder setName(Text name) {
            this.name = name;
            return this;
        }

        public FluidKeyBuilder setSprites(Identifier stillSprite, Identifier flowingSprite) {
            this.spriteId = stillSprite;
            this.flowingSpriteId = flowingSprite;
            return this;
        }

        public FluidKeyBuilder setRenderColor(int renderColor) {
            this.renderColor = renderColor;
            return this;
        }

        public FluidKeyBuilder setUnit(FluidUnit unit) {
            this.unit = unit;
            return this;
        }

        /** Adds the given unit to the set of units used. */
        public FluidKeyBuilder addUnit(FluidUnit unit) {
            this.unitSet.addUnit(unit);
            return this;
        }

        public FluidKeyBuilder setRawFluid(Fluid rawFluid) {
            this.rawFluid = rawFluid;
            return this;
        }

        /** Sets the {@link FluidKey#gaseous} property to true.
         * 
         * @return this. */
        public FluidKeyBuilder setGas() {
            gaseous = true;
            return this;
        }

        /** Sets the {@link FluidKey#gaseous} property to false.
         * 
         * @return this. */
        public FluidKeyBuilder setLiquid() {
            gaseous = false;
            return this;
        }

        /** Sets the {@link FluidKey#viscosity} property to the given {@link FluidAmount}, or null to use the default
         * value.
         * 
         * @return this. */
        public FluidKeyBuilder setViscosity(FluidAmount to) {
            if (to != null && to.isNegative()) {
                throw new IllegalArgumentException("Negative viscosity is not allowed! (" + to + ")");
            }
            this.viscosity = to;
            return this;
        }

        /** Sets the {@link FluidKey#netherViscosity} property to the given {@link FluidAmount}, or null to use the
         * default value.
         * 
         * @return this. */
        public FluidKeyBuilder setNetherViscosity(FluidAmount to) {
            if (to != null && to.isNegative()) {
                throw new IllegalArgumentException("Negative nether viscosity is not allowed! (" + to + ")");
            }
            this.netherViscosity = to;
            return this;
        }

        /** Sets the {@link FluidKey#cohesion} property to the given {@link FluidAmount}, or null to use the default
         * value.
         * 
         * @return this. */
        public FluidKeyBuilder setCohesion(FluidAmount to) {
            if (to != null && to.isNegative()) {
                throw new IllegalArgumentException("Negative cohesion is not allowed! (" + to + ")");
            }
            this.cohesion = to;
            return this;
        }

        /** Sets the {@link FluidKey#netherCohesion} property to the given {@link FluidAmount}, or null to use the
         * default value.
         * 
         * @return this. */
        public FluidKeyBuilder setNetherCohesion(FluidAmount to) {
            if (to != null && to.isNegative()) {
                throw new IllegalArgumentException("Negative nether cohesion is not allowed! (" + to + ")");
            }
            this.netherCohesion = to;
            return this;
        }

        /** Sets the {@link FluidKey#density} property to the given {@link FluidAmount}, or null to use the default
         * value.
         * 
         * @return this. */
        public FluidKeyBuilder setDensity(FluidAmount to) {
            if (to != null && to.isNegative()) {
                throw new IllegalArgumentException("Negative density is not allowed! (" + to + ")");
            }
            this.density = to;
            return this;
        }

        /** Sets the {@link FluidKey#thermalCapacity} property to the given {@link FluidAmount}, or null to use the
         * default value. (If this is null then it will default to the same value as
         * {@link FluidKeyBuilder#setDensity(FluidAmount)}).
         * 
         * @return this. */
        public FluidKeyBuilder setThermalCapacity(FluidAmount to) {
            if (to != null && to.isNegative()) {
                throw new IllegalArgumentException("Negative thermal capacity is not allowed! (" + to + ")");
            }
            this.thermalCapacity = to;
            return this;
        }
    }

    public FluidKey(FluidKeyBuilder builder) {
        if (builder.entry == null) {
            throw new NullPointerException("entry");
        }
        if (builder.unit == null) {
            throw new NullPointerException("unit");
        }
        if (builder.name == null) {
            throw new NullPointerException("name");
        }
        this.entry = builder.entry;
        this.unit = builder.unit;
        this.unitSet = builder.unitSet.copy();
        unitSet.addUnit(builder.unit);
        this.spriteId = builder.spriteId != null ? builder.spriteId : MISSING_SPRITE;
        this.flowingSpriteId = builder.flowingSpriteId != null ? builder.flowingSpriteId : spriteId;
        this.name = builder.name;
        this.renderColor = builder.renderColor;
        this.rawFluid = builder.rawFluid;
        if (rawFluid != null) {
            if (rawFluid instanceof EmptyFluid && rawFluid != Fluids.EMPTY) {
                throw new IllegalArgumentException("Different empty fluid!");
            }
            if (rawFluid instanceof BaseFluid && rawFluid != ((BaseFluid) rawFluid).getStill()) {
                throw new IllegalArgumentException("Only the still version of fluids are allowed!");
            }
        }
        this.gaseous = builder.gaseous;

        if (builder.viscosity != null) {
            this.viscosity = builder.viscosity;
        } else {
            this.viscosity = gaseous ? DEFAULT_GAS_VISCOSITY : FluidAmount.ONE;
        }
        this.netherViscosity = builder.netherViscosity != null ? builder.netherViscosity : viscosity;
        if (viscosity.isNegative()) {
            throw new IllegalArgumentException("Negative viscosity is not allowed (" + viscosity + ")");
        }
        if (netherViscosity.isNegative()) {
            throw new IllegalArgumentException("Negative nether viscosity is not allowed (" + netherViscosity + ")");
        }

        if (builder.cohesion != null) {
            this.cohesion = builder.cohesion;
        } else {
            this.cohesion = gaseous ? DEFAULT_GAS_COHESION : FluidAmount.ONE;
        }
        this.netherCohesion = builder.netherCohesion != null ? builder.netherCohesion : cohesion;
        if (netherCohesion.isNegative()) {
            throw new IllegalArgumentException("Negative nether cohesion is not allowed (" + netherCohesion + ")");
        }

        if (builder.density != null) {
            this.density = builder.density;
        } else {
            this.density = gaseous ? DEFAULT_GAS_DENSITY : FluidAmount.ONE;
        }
        if (density.isNegative()) {
            throw new IllegalArgumentException("Negative density is not allowed (" + density + ")");
        }

        if (builder.thermalCapacity != null) {
            this.thermalCapacity = builder.thermalCapacity;
        } else {
            this.thermalCapacity = density;
        }
        if (thermalCapacity.isNegative()) {
            throw new IllegalArgumentException("Negative thermal capacity is not allowed (" + thermalCapacity + ")");
        }

        validateClass(getClass());
    }

    public static FluidKey fromTag(CompoundTag tag) {
        if (tag.isEmpty()) {
            return FluidKeys.EMPTY;
        }
        FluidKey fluidKey = FluidKeys.get(FluidEntry.fromTag(tag));
        if (fluidKey == null) {
            return FluidKeys.EMPTY;
        }
        return fluidKey;
    }

    public final CompoundTag toTag() {
        return toTag(new CompoundTag());
    }

    public final CompoundTag toTag(CompoundTag tag) {
        if (isEmpty()) {
            return tag;
        }
        entry.toTag(tag);
        return tag;
    }

    // TODO: (Somehow) add compatibility with lib network stack
    // to optionally use it's caching mechanism to send network updates

    // It might be possible to just have two "write" + "read" methods, and
    // then just test to make sure it's possible to load that method even if
    // LNS isn't loaded.

    // Basically, it just has to work without crashing on load if LNS isn't found

    @Nullable
    public Fluid getRawFluid() {
        return rawFluid;
    }

    @Override
    public String toString() {
        return entry.toString();
    }

    public abstract FluidVolume readVolume(CompoundTag tag);

    public final boolean isEmpty() {
        return this == FluidKeys.EMPTY;
    }

    @Deprecated
    public FluidVolume withAmount(int amount) {
        return withAmount(FluidAmount.of1620(amount));
    }

    public FluidVolume withAmount(FluidAmount amount) {
        FluidVolume vol = withAmount(amount.as1620());
        vol.setAmount(amount);
        return vol;
    }

    /** Called when this is pumped out from the world. */
    public FluidVolume fromWorld(WorldView world, BlockPos pos) {
        return withAmount(FluidAmount.BUCKET);
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    private static void validateClass(Class<? extends FluidKey> clazz) {
        try {
            Method amountOld = clazz.getMethod("withAmount", int.class);
            Method amountNew = clazz.getMethod("withAmount", FluidAmount.class);

            if (amountOld.getDeclaringClass() == FluidKey.class) {
                if (amountNew.getDeclaringClass() == FluidKey.class) {
                    throw new IllegalStateException(
                        "The " + clazz + " must override at least 1 of {'FluidKey.withAmount(int)'"
                            + ", or 'FluidKey.withAmount(FluidAmount)' }"
                    );
                }
            }

        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to fine the methods during validation!", e);
        }
    }
}
