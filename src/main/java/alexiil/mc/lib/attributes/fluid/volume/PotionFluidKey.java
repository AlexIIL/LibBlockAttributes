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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

public final class PotionFluidKey extends FluidKey {

    public static final Identifier POTION_TEXTURE, FLOWING_POTION_TEXTURE;

    static {
        POTION_TEXTURE = LibBlockAttributes.id("fluid/potion");
        FLOWING_POTION_TEXTURE = LibBlockAttributes.id("fluid/potion_flowing");
    }

    public final Potion potion;

    /* package-private */ PotionFluidKey(Potion potion) {
        super(createKeyBuilder(potion));
        this.potion = potion;
    }

    private static FluidKeyBuilder createKeyBuilder(Potion potion) {
        FluidKeyBuilder builder = new FluidKeyBuilder();
        builder.setRegistryEntry(new FluidRegistryEntry<>(Registry.POTION, potion));
        builder.setSprites(POTION_TEXTURE, FLOWING_POTION_TEXTURE);
        builder.setName(new TranslatableText(potion.finishTranslationKey("item.minecraft.potion.effect.")));
        builder.setUnit(FluidUnit.BOTTLE);
        builder.setRenderColor(PotionUtil.getColor(potion));
        return builder;
    }

    @Override
    public PotionFluidVolume readVolume(CompoundTag tag) {
        return new PotionFluidVolume(this, tag);
    }

    @Override
    public PotionFluidVolume readVolume(JsonObject json) throws JsonSyntaxException {
        return new PotionFluidVolume(this, json);
    }

    /** @deprecated Replaced by {@link #withAmount(FluidAmount)}. */
    @Override
    @Deprecated
    public PotionFluidVolume withAmount(int amount) {
        return new PotionFluidVolume(this, amount);
    }

    @Override
    public PotionFluidVolume withAmount(FluidAmount amount) {
        return new PotionFluidVolume(this, amount);
    }
}
