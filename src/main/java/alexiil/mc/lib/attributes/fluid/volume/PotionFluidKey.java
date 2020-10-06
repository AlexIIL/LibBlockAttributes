/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
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
        builder.setUnit(FluidUnit.BOTTLE);
        int colour = PotionUtil.getColor(potion);
        TranslatableText text = new TranslatableText(potion.finishTranslationKey("item.minecraft.potion.effect."));
        builder.setName(text.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour))));
        builder.setRenderColor(colour);
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
    @Deprecated // (since = "0.6.0", forRemoval = true)
    public PotionFluidVolume withAmount(int amount) {
        return new PotionFluidVolume(this, amount);
    }

    @Override
    public PotionFluidVolume withAmount(FluidAmount amount) {
        return new PotionFluidVolume(this, amount);
    }

    @Override
    public void addTooltipExtras(FluidTooltipContext context, List<Text> tooltip) {
        super.addTooltipExtras(context, tooltip);
        PotionUtil.buildTooltip(PotionUtil.setPotion(new ItemStack(Items.POTION), potion), tooltip, 1.0F);
    }
}
