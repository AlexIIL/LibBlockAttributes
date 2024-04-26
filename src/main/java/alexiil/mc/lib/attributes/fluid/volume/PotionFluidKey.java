/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

public final class PotionFluidKey extends FluidKey {

    public static final Identifier POTION_TEXTURE, FLOWING_POTION_TEXTURE;

    static {
        POTION_TEXTURE = LibBlockAttributes.id("fluid/potion");
        FLOWING_POTION_TEXTURE = LibBlockAttributes.id("fluid/potion_flowing");
    }

    public final PotionContents potion;

    /* package-private */ PotionFluidKey(PotionContents potion) {
        super(createKeyBuilder(potion));
        this.potion = potion;
    }

    private static FluidKeyBuilder createKeyBuilder(PotionContents potion) {
        FluidKeyBuilder builder = new FluidKeyBuilder();
        builder.setRegistryEntry(new FluidRegistryEntry<>(Registries.POTION, potion.potion().orElse(Potions.WATER).value()));
        builder.setSprites(POTION_TEXTURE, FLOWING_POTION_TEXTURE);
        builder.setUnit(FluidUnit.BOTTLE);
        int colour = potion.getColor();
        MutableText text = Text.translatable(Potion.finishTranslationKey(potion.potion(), "item.minecraft.potion.effect."));
        builder.setName(text.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour))));
        builder.setRenderColor(colour);
        return builder;
    }

    @Override
    public PotionFluidVolume readVolume(NbtCompound tag) {
        return new PotionFluidVolume(this, tag);
    }

    @Override
    public PotionFluidVolume readVolume(JsonObject json) throws JsonSyntaxException {
        return new PotionFluidVolume(this, json);
    }

    /** @deprecated Replaced by {@link #withAmount(FluidAmount)}. */
    @Override
    @Deprecated(since = "0.6.0", forRemoval = true)
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
        PotionContentsComponent.buildTooltip(potion.getEffects(), tooltip::add, 1.0f, 20.0f);
    }
}
