/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import javax.annotation.Nonnull;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/** {@link ItemStack} equivalent for {@link Fluid fluids}. Instances must be constructed via
 * {@link FluidKeys#get(Fluid)}.{@link FluidKey#withAmount(FluidAmount) withAmount(FluidAmount)}. */
public class NormalFluidVolume extends FluidVolume {

    @Deprecated
    NormalFluidVolume(NormalFluidKey fluid, int amount) {
        super(fluid, amount);
    }

    NormalFluidVolume(NormalFluidKey fluid, FluidAmount amount) {
        super(fluid, amount);
    }

    NormalFluidVolume(NormalFluidKey fluid, CompoundTag tag) {
        super(fluid, tag);
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
