/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.WorldView;

import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.render.DefaultFluidVolumeRenderer;
import alexiil.mc.lib.attributes.fluid.volume.FluidEntry.FluidFloatingEntry;
import alexiil.mc.lib.attributes.fluid.volume.FluidTemperature.ContinuousFluidTemperature;
import alexiil.mc.lib.attributes.fluid.volume.FluidTemperature.DiscreteFluidTemperature;

import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;

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

    public static final JsonDeserializer<FluidKey> DESERIALIZER = (json, type, ctx) -> {
        if (json.isJsonNull()) {
            return FluidKeys.EMPTY;
        }
        if (json.isJsonPrimitive()) {
            if (json.getAsJsonPrimitive().isString()) {
                JsonObject wrapper = new JsonObject();
                wrapper.add("fluid", json);
                return fromJson(wrapper);
            }
        }
        if (!json.isJsonObject()) {
            throw new JsonSyntaxException("Expected " + json + " to be an object or a string!");
        }
        return fromJson(json.getAsJsonObject());
    };

    /** The identifier for this {@link FluidKey}. Primarily used during serialisation. */
    public final FluidEntry entry;

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

    /** How dense this fluid is.
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

    /** The amount of block light emitted from this fluid. 0-15. (Fluid tanks may use this to emit light, but it's not
     * required). */
    public final int luminosity;

    /** The temperature that applies to this {@link FluidKey}. This is updated if a {@link FluidProperty} is added which
     * contains a {@link FluidTemperature}. */
    @Nullable
    private FluidTemperature temperature;

    @Nullable
    private final Fluid rawFluid;

    /** Map of int index -> property. Ordered by the order that the properties were registered to
     * {@link #tryRegisterProperty(FluidProperty)}, so this might be different on the client and server. */
    /* package-private */ final List<FluidProperty<?>> properties = new ArrayList<>();

    /** Stable map of property -> int index. Ordered by {@link FluidProperty#COMPARATOR}, which ensures the key set
     * should be the same on the client and server. */
    /* package-private */ final Object2IntSortedMap<FluidProperty<?>> propertyKeys
        = new Object2IntAVLTreeMap<>(FluidProperty.COMPARATOR);

    /** A {@link FluidFilter} that only matches this {@link FluidKey}. */
    public final ExactFluidFilter exactFilter = new ExactFluidFilter(this);

    public static class FluidKeyBuilder {
        /* package-private */ FluidEntry entry;
        /* package-private */ Identifier spriteId;
        /* package-private */ Identifier flowingSpriteId;
        /* package-private */ Text name;
        /* package-private */ int renderColor = 0xFF_FF_FF;
        /* package-private */ int luminosity = 0;
        /* package-private */ FluidUnit unit = FluidUnit.BUCKET;
        /* package-private */ final FluidUnitSet unitSet = new FluidUnitSet();
        /* package-private */ Fluid rawFluid;
        /* package-private */ boolean gaseous = false;
        /* package-private */ FluidAmount viscosity, netherViscosity;
        /* package-private */ FluidAmount cohesion, netherCohesion;
        /* package-private */ FluidAmount density;
        /* package-private */ FluidAmount thermalCapacity;
        /* package-private */ FluidTemperature temperature;

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
         *            {@link DefaultedRegistry}, instead these instances will only be backed by a floating
         *            {@link Identifier}. */
        public FluidKeyBuilder setIdEntry(Identifier id) {
            this.entry = new FluidFloatingEntry(id);
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

        /** Sets the {@link FluidKey#luminosity} property.
         * 
         * @param luminance A value between 0 and 15. */
        public FluidKeyBuilder setLuminosity(int luminance) {
            this.luminosity = Math.max(0, Math.min(15, luminance));
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

        /** Sets the {@link FluidKey#thermalCapacity} property to the given {@link FluidAmount}, or null to allow
         * {@link FluidProperty}s to provide the fluid for this fluid.
         * 
         * @return this. */
        public FluidKeyBuilder setTemperature(DiscreteFluidTemperature temperature) {
            this.temperature = temperature;
            return this;
        }

        /** Sets the {@link FluidKey#thermalCapacity} property to the given {@link FluidAmount}, or null to allow
         * {@link FluidProperty}s to provide the fluid for this fluid.
         * 
         * @return this. */
        public FluidKeyBuilder setTemperature(ContinuousFluidTemperature temperature) {
            this.temperature = temperature;
            return this;
        }
    }

    public FluidKey(FluidKeyBuilder builder) {
        if (builder.entry == null) {
            throw new NullPointerException(
                "The builder is missing the 'entry' property! Did you forget to call either 'setRegistryEntry' or 'setIdEntry'?"
            );
        }
        if (builder.unit == null) {
            throw new NullPointerException("The builder is missing it's primary unit!");
        }
        if (builder.name == null) {
            throw new NullPointerException("The builder is missing it's name!");
        }
        this.entry = builder.entry;
        this.unit = builder.unit;
        this.unitSet = builder.unitSet.copy();
        unitSet.addUnit(builder.unit);
        this.spriteId = builder.spriteId != null ? builder.spriteId : MISSING_SPRITE;
        this.flowingSpriteId = builder.flowingSpriteId != null ? builder.flowingSpriteId : spriteId;
        this.name = builder.name;
        this.renderColor = builder.renderColor;
        this.luminosity = builder.luminosity;
        this.rawFluid = builder.rawFluid;
        if (rawFluid != null) {
            if (rawFluid instanceof EmptyFluid && rawFluid != Fluids.EMPTY) {
                throw new IllegalArgumentException("Different empty fluid!");
            }
            if (rawFluid instanceof FlowableFluid && rawFluid != ((FlowableFluid) rawFluid).getStill()) {
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

        if (this instanceof FluidTemperature) {
            this.temperature = (FluidTemperature) this;
        } else {
            this.temperature = builder.temperature;
        }
        FluidTemperature.validate(temperature);

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

    public static FluidKey fromJson(JsonObject json) throws JsonSyntaxException {
        try {
            return fromJsonInternal(json);
        } catch (JsonSyntaxException jse) {
            throw new JsonSyntaxException("Not a valid fluid key: " + json, jse);
        }
    }

    /** The inverse of {@link #fromJson(JsonObject)}. */
    public final JsonObject toJson() {
        JsonObject json = new JsonObject();
        toJson(json);
        return json;
    }

    /** The inverse of {@link #fromJson(JsonObject)}.
     * 
     * @param json The {@link JsonObject} to modify. */
    public final void toJson(JsonObject json) {
        if (entry instanceof FluidFloatingEntry) {
            json.addProperty("floating_fluid", entry.getId().toString());
        } else {
            FluidRegistryEntry<?> reg = (FluidRegistryEntry<?>) entry;
            if (reg.backingRegistry == Registry.FLUID) {
                json.addProperty("fluid", reg.getId().toString());
            } else if (reg.backingRegistry == Registry.POTION) {
                json.addProperty("potion", reg.getId().toString());
            } else {
                JsonObject sub = new JsonObject();
                json.add("fluid", sub);
                sub.addProperty("registry", reg.getRegistryInternalName());
                sub.addProperty("id", reg.getId().toString());
            }
        }
        assert this.equals(fromJson(json)) : json.toString();
    }

    /** Reads a {@link FluidKey} from a vanilla minecraft {@link PacketByteBuf}. */
    public static FluidKey fromMcBuffer(PacketByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return FluidKeys.EMPTY;
        }
        return FluidKeys.get(FluidEntry.fromMcBuffer(buffer));
    }

    /** Writes this {@link FluidKey} to a vanilla minecraft {@link PacketByteBuf}, such that it can be read with
     * {@link #fromMcBuffer(PacketByteBuf)}. */
    public final void toMcBuffer(PacketByteBuf buffer) {
        if (isEmpty()) {
            buffer.writeBoolean(false);
            return;
        }
        buffer.writeBoolean(true);
        entry.toMcBuffer(buffer);
    }

    // TODO: LNS NetByteBuf read/write!

    /* package-private */ static FluidKey fromJsonInternal(JsonObject json) throws JsonSyntaxException {
        if (json.has("floating_fluid")) {
            if (json.has("potion") || json.has("fluid")) {
                throw new JsonSyntaxException(
                    "Expected only one of 'fluid' or 'potion' or 'floating_fluid', but got multiple!"
                );
            }
            JsonElement j = json.get("floating_fluid");
            if (!j.isJsonPrimitive()) {
                throw new JsonSyntaxException("Expected 'floating_fluid' to be a string, but got " + j);
            }
            Identifier id = getAsIdentifier(json.get("floating_fluid"), "floating_fluid");
            FluidFloatingEntry entry = new FluidFloatingEntry(id);
            FluidKey fluidKey = FluidKeys.get(entry);
            if (fluidKey == null) {
                throw throwBadEntryException(
                    "floating_fluid", "floating fluid identifier", "floating fluid identifiers", id.toString(),
                    FluidKeys.getFloatingFluidIds()
                );
            }
            return fluidKey;
        } else if (json.has("potion")) {
            if (json.has("fluid")) {
                throw new JsonSyntaxException(
                    "Expected 'fluid' or 'potion' or 'floating_fluid', but got both! You should use one or the other, not both"
                );
            }
            return FluidKeys.get(getRegistryEntry(json.get("potion"), "potion", "potions", Registry.POTION));
        } else if (json.has("fluid")) {
            JsonElement jFluid = json.get("fluid");
            if (!jFluid.isJsonObject()) {
                return FluidKeys.get(getRegistryEntry(jFluid, "fluid", "fluids", Registry.FLUID));
            } else {
                JsonObject obj = jFluid.getAsJsonObject();
                Registry<?> reg = getRegistryEntry(obj.get("registry"), "registry", "registries", Registry.REGISTRIES);
                return fromRegistry(obj, reg);
            }
        } else {
            throw new JsonSyntaxException(
                "Expected 'fluid' or 'potion' or 'floating_fluid', but got nothing! (" + json + ")"
            );
        }
    }

    private static <T> FluidKey fromRegistry(JsonObject obj, Registry<T> reg) {
        T entry = getRegistryEntry(obj, "id", "ids", reg);
        FluidRegistryEntry<T> fluidEntry = new FluidRegistryEntry<>(reg, entry);
        FluidKey fluidKey = FluidKeys.get(fluidEntry);
        if (fluidKey == null) {
            throw throwBadEntryException(
                "id", "ids", "ids", fluidEntry.getId().toString(), FluidKeys.getRegistryFluidIds()
            );
        }
        return fluidKey;
    }

    private static <T> T getRegistryEntry(JsonElement json, String key, String keys, Registry<T> registry) {
        if (json == null) {
            throw new JsonSyntaxException("Expected '" + key + "' to be a string, but got nothing!");
        }
        Identifier id = getAsIdentifier(json, key);

        if (registry.containsId(id)) {
            T value = registry.get(id);
            if (value != null) {
                if (registry instanceof DefaultedRegistry<?>) {
                    if (value != registry.get(((DefaultedRegistry<?>) registry).getDefaultId())) {
                        return value;
                    }
                } else {
                    return value;
                }
            }
        }

        throw throwBadEntryException(key, key, keys, id.toString(), registry.getIds());
    }

    private static JsonSyntaxException throwBadEntryException(
        String key, String name, String keys, String found, Set<?> src
    ) {
        StringBuilder error = new StringBuilder();
        error.append("Expected '");
        error.append(key);
        error.append("' to be a valid ");
        error.append(name);
        error.append(", but got '");
        error.append(found);
        int size = src.size();
        if (size == 0) {
            error.append("'(");
            error.append("no valid ");
            error.append(keys);
            error.append("!)");
            throw new JsonSyntaxException(error.toString());
        }
        error.append("'!\n\t(");
        error.append(size);
        error.append(" valid ");
        error.append(keys);
        error.append(":");

        Object[] dest = new Object[size];
        src.toArray(dest);
        for (int i = 0; i < dest.length; i++) {
            if (dest[i] instanceof FluidEntry) {
                dest[i] = ((FluidEntry) dest[i]).getId();
            }
            dest[i] = dest[i].toString();
        }
        Arrays.sort(dest);

        for (Object str : dest) {
            error.append("\n\t - '");
            error.append(str);
            error.append("'");
        }
        error.append("\n)");

        throw new JsonSyntaxException(error.toString());
    }

    private static Identifier getAsIdentifier(JsonElement json, String key) {
        if (!(json.isJsonPrimitive())) {
            throw new JsonSyntaxException("Expected '" + key + "' to be a string, but got " + json);
        }
        String str = json.getAsJsonPrimitive().getAsString();
        if (!str.contains(":")) {
            throw new JsonSyntaxException(
                "Expected '" + key + "' to be a string with the " + key + " identifier, but got '" + str + "'!"
            );
        }
        Identifier id = Identifier.tryParse(str);
        if (id == null) {
            throw new JsonSyntaxException("Expected '" + key + "' to be a valid identifier, but got '" + str + "'!");
        }
        return id;
    }

    /** Registers this {@link FluidKey} into the {@link FluidKeys} registry.
     * <p>
     * You should only ever register a {@link FluidKey}'s {@link FluidEntry} into the registry once, so it may not be
     * necessary to call this method. As such you should only call this on {@link FluidKey}s that you have created. */
    public final void register() {
        if (entry instanceof FluidFloatingEntry) {
            FluidKeys.put((FluidFloatingEntry) entry, this);
        } else {
            FluidKeys.put((FluidRegistryEntry<?>) entry, this);
        }
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

    /** @return The {@link FluidTemperature} for this fluid, or null if neither this fluid nor any of it's properties
     *         provide the temperature. */
    public final FluidTemperature getTemperature() {
        return temperature;
    }

    /** @return Null if the {@link FluidProperty} was registered successfully, or a non-null error message containing
     *         the reason why it couldn't be added. */
    @Nullable
    @CheckReturnValue
    public final String tryRegisterProperty(FluidProperty<?> property) {
        if (temperature != null && property.temperature != null) {
            return "Cannot have multiple temperature sources for this fluid! (\n\tFluidKey = " + this
                + ",\n\tFluidKey.temperature = " + temperature + ",\n\tFluidProperty = " + property
                + ",\n\tFluidProperty.temperature = " + property.temperature;
        }
        for (FluidProperty<?> existing : properties) {
            if (existing.id.equals(property.id)) {
                return "Tried to register multiple properties with an ID of " + existing.id + "!";
            }
        }
        propertyKeys.put(property, properties.size());
        properties.add(property);
        if (temperature != null) {
            temperature = property.temperature;
        }
        return null;
    }

    /** Forcibly attempts to register the given property, throwing an exception if the registration fails. */
    public final void forceRegisterProperty(FluidProperty<?> property) {
        String error = tryRegisterProperty(property);
        if (error != null) {
            throw new IllegalStateException(error);
        }
    }

    /** @return Every {@link FluidProperty} that as been {@link #tryRegisterProperty(FluidProperty) registered} to this
     *         {@link FluidKey}. The returned set is ordered by {@link FluidProperty#id}.{@link Identifier#toString()
     *         toString()}, to allow server-client sync to happen correctly. */
    public final SortedSet<FluidProperty<?>> getProperties() {
        return Collections.unmodifiableSortedSet(propertyKeys.keySet());
    }

    public abstract FluidVolume readVolume(CompoundTag tag);

    public FluidVolume readVolume(JsonObject json) throws JsonSyntaxException {
        if (isEmpty()) {
            return FluidVolumeUtil.EMPTY;
        }
        JsonElement amount = json.get("amount");
        if (amount == null) {
            return FluidVolumeUtil.EMPTY;
        }
        return withAmount(FluidVolume.parseAmount(amount));
    }

    /** Reads a {@link FluidVolume} from the {@link PacketByteBuf}. Unfortunately this method is not particularly
     * efficient at encoding */
    public final FluidVolume readVolume(PacketByteBuf buffer) {
        if (isEmpty()) {
            return FluidVolumeUtil.EMPTY;
        }
        FluidAmount amount = FluidAmount.fromMcBuffer(buffer);
        FluidVolume volume = createFromMcBuffer(buffer, amount);
        volume.readProperties(buffer);
        return volume;
    }

    /** Creates a new {@link FluidVolume} and reads it from the buffer with
     * {@link FluidVolume#fromMcBufferInternal(PacketByteBuf)}. This should not attempt to read properties. */
    protected FluidVolume createFromMcBuffer(PacketByteBuf buffer, FluidAmount amount) {
        FluidVolume volume = withAmount(amount);
        volume.fromMcBufferInternal(buffer);
        return volume;
    }

    // TODO: Add LNS support!

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

    /** @return The full tooltip for this {@link FluidKey}. This returns an empty list if this is the empty fluid, or
     *         the name and then the extra data from {@link #addTooltipExtras(List)}. */
    public final List<Text> getFullTooltip() {
        return getFullTooltip(FluidTooltipContext.USE_CONFIG);
    }

    /** @return The full tooltip for this {@link FluidKey}. This returns an empty list if this is the empty fluid, or
     *         the name and then the extra data from {@link #addTooltipExtras(FluidTooltipContext, List)}. */
    public final List<Text> getFullTooltip(FluidTooltipContext context) {
        if (isEmpty()) {
            return Collections.emptyList();
        }
        List<Text> tooltip = new ArrayList<>();
        addFullTooltip(tooltip, context);
        return tooltip;
    }

    public final void addFullTooltip(List<Text> tooltip, FluidTooltipContext context) {
        if (!isEmpty()) {
            tooltip.add(context.stripFluidColours(name));
            addTooltipExtras(context, tooltip);
            addTooltipTemperature(context, tooltip);
            addTooltipProperties(context, tooltip);
        }
    }

    /** Add extra data to the tooltip. */
    public final void addTooltipExtras(List<Text> tooltip) {
        addTooltipExtras(FluidTooltipContext.USE_CONFIG, tooltip);
    }

    /** Add extra data to the tooltip. */
    public void addTooltipExtras(FluidTooltipContext context, List<Text> tooltip) {
        if (context.isAdvanced()) {
            tooltip.add(new LiteralText(entry.getRegistryInternalName()).formatted(Formatting.DARK_GRAY));
            tooltip.add(new LiteralText(entry.getId().toString()).formatted(Formatting.DARK_GRAY));
        }
    }

    public final void addTooltipTemperature(FluidTooltipContext context, List<Text> tooltip) {
        if (temperature != null) {
            temperature.addTemperatureToTooltip(this, context, tooltip);
        }
    }

    public final void addTooltipProperties(List<Text> tooltip) {
        addTooltipProperties(FluidTooltipContext.USE_CONFIG, tooltip);
    }

    public final void addTooltipProperties(FluidTooltipContext context, List<Text> tooltip) {
        for (FluidProperty<?> prop : properties) {
            prop.addTooltipExtras(this, context, tooltip);
        }
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
            throw new IllegalStateException("Failed to find the methods during validation!", e);
        }
    }
}
