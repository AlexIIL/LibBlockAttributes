/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import javax.annotation.Nonnull;

import net.minecraft.fluid.BaseFluid;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/** Identical to {@link NormalFluidVolume}, but without an amount and with extra data hidden from public view. As such
 * this is safe to use in normal maps and sets. */
public class NormalFluidKey extends FluidKey {

    /** @deprecated As the flowing sprite ID is needed as well. */
    @Deprecated
    public static NormalFluidKeyBuilder builder(Fluid fluid, Identifier spriteId, Text name) {
        return new NormalFluidKeyBuilder(fluid, spriteId, name);
    }

    public static NormalFluidKeyBuilder builder(
        Fluid fluid, Identifier spriteId, Identifier flowingSpriteId, Text name
    ) {
        return new NormalFluidKeyBuilder(fluid, spriteId, flowingSpriteId, name);
    }

    public static class NormalFluidKeyBuilder extends FluidKeyBuilder {

        public final Fluid fluid;

        /** @deprecated As the flowing sprite ID is needed as well. */
        @Deprecated
        public NormalFluidKeyBuilder(Fluid fluid, Identifier spriteId, Text name) {
            super(new FluidRegistryEntry<>(Registry.FLUID, fluid), spriteId, name);
            this.fluid = fluid;
        }

        public NormalFluidKeyBuilder(Fluid fluid, Identifier spriteId, Identifier flowingSpriteId, Text name) {
            super(new FluidRegistryEntry<>(Registry.FLUID, fluid), spriteId, flowingSpriteId, name);
            this.fluid = fluid;
        }

        @Override
        public NormalFluidKeyBuilder setRenderColor(int renderColor) {
            return (NormalFluidKeyBuilder) super.setRenderColor(renderColor);
        }

        @Override
        public NormalFluidKeyBuilder setUnit(FluidUnit unit) {
            return (NormalFluidKeyBuilder) super.setUnit(unit);
        }

        @Override
        public NormalFluidKeyBuilder addUnit(FluidUnit unit) {
            return (NormalFluidKeyBuilder) super.addUnit(unit);
        }

        public NormalFluidKey build() {
            return new NormalFluidKey(this);
        }
    }

    @Nonnull
    public final Fluid fluid;

    public NormalFluidKey(NormalFluidKeyBuilder builder) {
        super(builder);
        Fluid fl = builder.fluid;
        if (fl == null) {
            throw new NullPointerException("fluid");
        }
        if (fl instanceof EmptyFluid && fl != Fluids.EMPTY) {
            throw new IllegalArgumentException("Different empty fluid!");
        }
        if (fl instanceof BaseFluid && fl != ((BaseFluid) fl).getStill()) {
            throw new IllegalArgumentException("Only the still version of fluids are allowed!");
        }
        this.fluid = fl;
    }

    @Override
    public Fluid getRawFluid() {
        return fluid;
    }

    @Override
    public NormalFluidVolume withAmount(int amount) {
        return new NormalFluidVolume(this, amount);
    }

    @Override
    public NormalFluidVolume readVolume(CompoundTag tag) {
        return new NormalFluidVolume(this, tag);
    }
}
