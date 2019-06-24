/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class PotionFluidKey extends FluidKey {

    public static final Identifier POTION_TEXTURE;

    static {
        // potion_glint = FF_80_40_CC
        // @ -50 around Z
        // + 10 around Z
        POTION_TEXTURE = new Identifier("libblockattributes", "fluid/potion");
    }

    public final Potion potion;

    /* package-private */ PotionFluidKey(Potion potion) {
        super(new FluidKeyBuilder(new FluidRegistryEntry<>(Registry.POTION, potion), //
            POTION_TEXTURE, //
            new TranslatableComponent(potion.getName("item.minecraft.potion.effect."))//
        ).setUnit(FluidUnit.BOTTLE).setRenderColor(PotionUtil.getColor(potion)));
        this.potion = potion;
    }

    @Override
    public PotionFluidVolume readVolume(CompoundTag tag) {
        return new PotionFluidVolume(this, tag);
    }

    @Override
    public PotionFluidVolume withAmount(int amount) {
        return new PotionFluidVolume(this, amount);
    }
}
