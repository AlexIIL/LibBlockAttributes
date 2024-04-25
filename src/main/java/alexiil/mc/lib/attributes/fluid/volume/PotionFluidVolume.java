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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.render.DefaultFluidVolumeRenderer;
import alexiil.mc.lib.attributes.fluid.render.EnchantmentGlintFluidRenderer;
import alexiil.mc.lib.attributes.fluid.render.FluidVolumeRenderer;

public final class PotionFluidVolume extends FluidVolume {

    @Deprecated(since = "0.6.0", forRemoval = true)
    public PotionFluidVolume(PotionFluidKey key, int amount) {
        super(key, amount);
    }

    public PotionFluidVolume(PotionFluidKey key, FluidAmount amount) {
        super(key, amount);
    }

    public PotionFluidVolume(PotionFluidKey key, NbtCompound tag) {
        super(key, tag);
    }

    public PotionFluidVolume(PotionFluidKey key, JsonObject json) throws JsonSyntaxException {
        super(key, json);
    }

    public RegistryEntry<Potion> getPotion() {
        return getFluidKey().potion;
    }

    @Override
    public PotionFluidKey getFluidKey() {
        return (PotionFluidKey) fluidKey;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public FluidVolumeRenderer getRenderer() {
        if (getPotion().value().getEffects().isEmpty()) {
            return DefaultFluidVolumeRenderer.INSTANCE;
        } else {
            return EnchantmentGlintFluidRenderer.INSTANCE;
        }
    }
}
