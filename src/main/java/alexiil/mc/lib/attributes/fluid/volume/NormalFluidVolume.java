/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import javax.annotation.Nonnull;

import com.google.gson.JsonObject;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/** {@link ItemStack} equivalent for {@link Fluid fluids}. Instances must be constructed via
 * {@link FluidKeys#get(Fluid)}.{@link FluidKey#withAmount(FluidAmount) withAmount(FluidAmount)}.
 * 
 * @deprecated Because any {@link FluidKey} can map to a single {@link Fluid}, and {@link SimpleFluidVolume} has a much
 *             better name than this. */
@Deprecated // (since = "0.6.0", forRemoval = true)
public class NormalFluidVolume extends FluidVolume {

    @Deprecated // (since = "0.6.0", forRemoval = true)
    NormalFluidVolume(NormalFluidKey fluid, int amount) {
        super(fluid, amount);
    }

    NormalFluidVolume(NormalFluidKey fluid, FluidAmount amount) {
        super(fluid, amount);
    }

    NormalFluidVolume(NormalFluidKey fluid, CompoundTag tag) {
        super(fluid, tag);
    }

    NormalFluidVolume(NormalFluidKey fluid, JsonObject json) {
        super(fluid, json);
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
