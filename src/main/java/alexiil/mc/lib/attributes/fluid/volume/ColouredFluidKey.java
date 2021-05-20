/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

public class ColouredFluidKey extends FluidKey {

    public final float defaultRed, defaultGreen, defaultBlue, defaultAlpha;
    public final float minRed, minGreen, minBlue, minAlpha;
    public final float maxRed, maxGreen, maxBlue, maxAlpha;

    public ColouredFluidKey(ColouredFluidKeyBuilder builder) {
        super(builder);

        this.minRed = validateMin("red", builder.minRed);
        this.minGreen = validateMin("green", builder.minGreen);
        this.minBlue = validateMin("blue", builder.minBlue);
        this.minAlpha = validateMin("alpha", builder.minAlpha);

        this.maxRed = validateMax("red", minRed, builder.maxRed);
        this.maxGreen = validateMax("green", minGreen, builder.maxGreen);
        this.maxBlue = validateMax("blue", minBlue, builder.maxBlue);
        this.maxAlpha = validateMax("alpha", minAlpha, builder.maxAlpha);

        this.defaultRed = validateDefault("red", minRed, maxRed, builder.defaultRed);
        this.defaultGreen = validateDefault("red", minGreen, maxGreen, builder.defaultGreen);
        this.defaultBlue = validateDefault("blue", minBlue, maxBlue, builder.defaultBlue);
        this.defaultAlpha = validateDefault("alpha", minAlpha, maxAlpha, builder.defaultAlpha);
    }

    private static float validateMin(String name, float min) {
        if (min < 0) {
            throw new IllegalArgumentException("Minimum " + name + " was less than 0! (Was " + min + ")");
        }
        if (min > 1) {
            throw new IllegalArgumentException("Minimum " + name + " was greater than 1! (Was " + min + ")");
        }
        return min;
    }

    private static float validateMax(String name, float min, float max) {
        if (max < 0) {
            throw new IllegalArgumentException("Maximum " + name + " was less than 0! (Was " + max + ")");
        }
        if (max > 1) {
            throw new IllegalArgumentException("Maximum " + name + " was greater than 1! (Was " + max + ")");
        }
        if (max < min) {
            throw new IllegalArgumentException(
                "Maximum " + name + " was less than minimum! (Max = " + max + ", min = " + min + ")"
            );
        }
        return max;
    }

    private static float validateDefault(String name, float min, float max, float value) {
        if (value < min) {
            throw new IllegalArgumentException(
                "Default " + name + " was less than minimum! (Max = " + max + ", min = " + min + " default = " + value
                    + ")"
            );
        }
        if (value > max) {
            throw new IllegalArgumentException(
                "Default " + name + " was greater than maximum! (Max = " + max + ", min = " + min + " default = "
                    + value + ")"
            );
        }
        return value;
    }

    public static class ColouredFluidKeyBuilder extends FluidKeyBuilder {
        float defaultRed = 1, defaultGreen = 1, defaultBlue = 1, defaultAlpha = 1;

        float minRed = 0, maxRed = 1;
        float minGreen = 0, maxGreen = 1;
        float minBlue = 0, maxBlue = 1;
        float minAlpha = 1 / 255f, maxAlpha = 1;

        public ColouredFluidKeyBuilder() {
            super();
        }

        public ColouredFluidKeyBuilder(Identifier id) {
            super(id);
        }

        public ColouredFluidKeyBuilder(Fluid fluid) {
            super(fluid);
        }

        public ColouredFluidKeyBuilder(
            FluidRegistryEntry<?> registryEntry, Identifier spriteId, Identifier flowingSpriteId, Text name
        ) {
            super(registryEntry, spriteId, flowingSpriteId, name);
        }

        @Override
        public ColouredFluidKeyBuilder copyFrom(FluidKeyBuilder from) {
            super.copyFrom(from);
            if (from instanceof ColouredFluidKeyBuilder) {
                ColouredFluidKeyBuilder o = (ColouredFluidKeyBuilder) from;
                this.defaultRed = o.defaultRed;
                this.defaultGreen = o.defaultGreen;
                this.defaultBlue = o.defaultBlue;
                this.defaultAlpha = o.defaultAlpha;
            }
            return this;
        }

        public ColouredFluidKeyBuilder setDefaultColour(float red, float green, float blue) {
            return setDefaultColour(red, green, blue, 1);
        }

        public ColouredFluidKeyBuilder setDefaultColour(float red, float green, float blue, float alpha) {
            this.defaultRed = red;
            this.defaultGreen = green;
            this.defaultBlue = blue;
            this.defaultAlpha = alpha;
            return this;
        }

        public ColouredFluidKeyBuilder setRgbBounds(float min, float max) {
            minRed = minGreen = minBlue = min;
            maxRed = maxGreen = maxBlue = max;
            return this;
        }

        public ColouredFluidKeyBuilder setRedBounds(float min, float max) {
            minRed = min;
            maxRed = max;
            return this;
        }

        public ColouredFluidKeyBuilder setGreenBounds(float min, float max) {
            minGreen = min;
            maxGreen = max;
            return this;
        }

        public ColouredFluidKeyBuilder setBlueBounds(float min, float max) {
            minBlue = min;
            maxBlue = max;
            return this;
        }

        public ColouredFluidKeyBuilder setAlphaBounds(float min, float max) {
            minAlpha = min;
            maxAlpha = max;
            return this;
        }
    }

    @Override
    public ColouredFluidVolume readVolume(NbtCompound tag) {
        return new ColouredFluidVolume(this, tag);
    }

    @Override
    public ColouredFluidVolume readVolume(JsonObject json) throws JsonSyntaxException {
        return new ColouredFluidVolume(this, json);
    }

    @Override
    public ColouredFluidVolume withAmount(FluidAmount amount) {
        return new ColouredFluidVolume(this, amount);
    }

    @Override
    protected ColouredFluidVolume createFromMcBuffer(PacketByteBuf buffer, FluidAmount amount) {
        ColouredFluidVolume volume = withAmount(amount);
        volume.fromMcBufferInternal(buffer);
        return volume;
    }
}
