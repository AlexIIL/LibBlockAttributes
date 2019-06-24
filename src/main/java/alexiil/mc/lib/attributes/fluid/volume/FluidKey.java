/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import javax.annotation.Nullable;

import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ViewableWorld;

public abstract class FluidKey {

    /* package-private */ final FluidRegistryEntry<?> registryEntry;

    /** The units to use when displaying amounts, capacities, and flow rates to the player. */
    public final FluidUnit unit;

    /** The sprite to use when rendering this {@link FluidKey}'s specifically.
     * <p>
     * Note that this might differ from the one returned by {@link FluidVolume#getSprite()}! */
    public final Identifier spriteId;

    /** The colour to use when rendering this {@link FluidKey}'s specifically.
     * <p>
     * Note that this might differ from the one returned by {@link FluidVolume#getRenderColor()}! */
    public final int renderColor;

    /** The name to use when displaying tooltips for this {@link FluidKey} specifically.
     * <p>
     * Note that this might differ from the one returned by {@link FluidVolume#getName()}! */
    public final Component name;

    public static class FluidKeyBuilder {
        /* package-private */ final FluidRegistryEntry<?> registryEntry;
        /* package-private */ final Identifier spriteId;
        /* package-private */ final Component name;
        /* package-private */ int renderColor = 0xFF_FF_FF;
        /* package-private */ FluidUnit unit = FluidUnit.BUCKET;

        public FluidKeyBuilder(FluidRegistryEntry<?> registryEntry, Identifier spriteId, Component name) {
            this.registryEntry = registryEntry;
            this.spriteId = spriteId;
            this.name = name;
        }

        public FluidKeyBuilder setRenderColor(int renderColor) {
            this.renderColor = renderColor;
            return this;
        }

        public FluidKeyBuilder setUnit(FluidUnit unit) {
            this.unit = unit;
            return this;
        }
    }

    public FluidKey(FluidKeyBuilder builder) {
        if (builder.registryEntry == null) {
            throw new NullPointerException("registryEntry");
        }
        if (builder.unit == null) {
            throw new NullPointerException("unit");
        }
        if (builder.spriteId == null) {
            throw new NullPointerException("spriteId");
        }
        if (builder.name == null) {
            throw new NullPointerException("textComponent");
        }
        this.registryEntry = builder.registryEntry;
        this.unit = builder.unit;
        this.spriteId = builder.spriteId;
        this.name = builder.name;
        this.renderColor = builder.renderColor;
    }

    public static FluidKey fromTag(CompoundTag tag) {
        if (tag.isEmpty()) {
            return FluidKeys.EMPTY;
        }
        FluidKey fluidKey = FluidKeys.get(FluidRegistryEntry.fromTag(tag));
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
        registryEntry.toTag(tag);
        return tag;
    }

    @Nullable
    public Fluid getRawFluid() {
        return null;
    }

    @Override
    public String toString() {
        return registryEntry.toString();
    }

    public abstract FluidVolume readVolume(CompoundTag tag);

    public final boolean isEmpty() {
        return this == FluidKeys.EMPTY;
    }

    public abstract FluidVolume withAmount(int amount);

    /** Called when this is pumped out from the world. */
    public FluidVolume fromWorld(ViewableWorld world, BlockPos pos) {
        return withAmount(FluidVolume.BUCKET);
    }
}
