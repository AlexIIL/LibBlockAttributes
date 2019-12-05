/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

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
        // potion_glint = FF_80_40_CC
        // @ -50 around Z
        // + 10 around Z
        POTION_TEXTURE = LibBlockAttributes.id("fluid/potion");
        FLOWING_POTION_TEXTURE = LibBlockAttributes.id("fluid/potion_flowing");
    }

    public final Potion potion;

    /* package-private */ PotionFluidKey(Potion potion) {
        super(
            new FluidKeyBuilder(
                new FluidRegistryEntry<>(Registry.POTION, potion), POTION_TEXTURE, FLOWING_POTION_TEXTURE,
                new TranslatableText(potion.getName("item.minecraft.potion.effect."))
            ).setUnit(FluidUnit.BOTTLE).setRenderColor(PotionUtil.getColor(potion))
        );
        this.potion = potion;
    }

    @Override
    public PotionFluidVolume readVolume(CompoundTag tag) {
        return new PotionFluidVolume(this, tag);
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
