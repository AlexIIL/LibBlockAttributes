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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

/** {@link ItemStack} equivalent for {@link Fluid fluids}. Instances must be constructed via
 * {@link FluidKeys#get(Fluid)}.{@link FluidKey#withAmount(int) withAmount(int)}. */
public class NormalFluidVolume extends FluidVolume {

    NormalFluidVolume(NormalFluidKey fluid, int amount) {
        super(fluid, amount);
        if (fluid.fluid instanceof EmptyFluid && fluid != FluidKeys.EMPTY) {
            throw new IllegalArgumentException("Different empty fluid!");
        }
        if (fluid.fluid instanceof BaseFluid && fluid.fluid != ((BaseFluid) fluid.fluid).getStill()) {
            throw new IllegalArgumentException("Only the still version of fluids are allowed!");
        }
    }

    NormalFluidVolume(NormalFluidKey fluid, CompoundTag tag) {
        super(fluid, tag);
        if (fluid == null) {
            throw new NullPointerException("fluid");
        }
        if (fluid.fluid instanceof EmptyFluid && fluid != FluidKeys.EMPTY) {
            throw new IllegalArgumentException("Different empty fluid!");
        }
        if (fluid.fluid instanceof BaseFluid && fluid.fluid != ((BaseFluid) fluid.fluid).getStill()) {
            throw new IllegalArgumentException("Only the still version of fluids are allowed!");
        }
    }

    @Override
    @Nonnull
    public final Fluid getRawFluid() {
        return getFluidKey().fluid;
    }

    @Override
    public NormalFluidKey getFluidKey() {
        return (NormalFluidKey) fluidKey;
    }
}
