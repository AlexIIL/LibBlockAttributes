/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.mixin.impl;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;

import alexiil.mc.lib.attributes.fluid.FluidProviderItem;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.mixin.api.IBucketItem;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.fluid.volume.PotionContents;
import alexiil.mc.lib.attributes.fluid.volume.PotionFluidKey;
import alexiil.mc.lib.attributes.misc.Ref;

@Mixin(PotionItem.class)
public class PotionItemMixin extends Item implements FluidProviderItem, IBucketItem {

    public PotionItemMixin(Item.Settings settings) {
        super(settings);
    }

    @Override
    public FluidVolume drain(Ref<ItemStack> stack) {
        PotionContents potion = PotionContents.fromStack(stack.obj);
        if (potion == null) {
            return FluidVolumeUtil.EMPTY;
        }

        FluidKey fluidKey = FluidKeys.get(potion);
        if (fluidKey == null) {
            return FluidVolumeUtil.EMPTY;
        }
        stack.obj = new ItemStack(Items.GLASS_BOTTLE);
        return fluidKey.withAmount(FluidVolume.BOTTLE);
    }

    @Override
    public boolean fill(Ref<ItemStack> stack, Ref<FluidVolume> with) {
        return false;
    }

    @Override
    public boolean libblockattributes__shouldExposeFluid() {
        return true;
    }

    @Override
    public FluidKey libblockattributes__getFluid(ItemStack stack) {
        PotionContents potion = PotionContents.fromStack(stack);
        if (potion == null) {
            return FluidKeys.EMPTY;
        }

        return FluidKeys.get(potion);
    }

    @Override
    public ItemStack libblockattributes__withFluid(FluidKey fluid) {
        PotionContents potion;
        if (fluid instanceof PotionFluidKey) {
            potion = ((PotionFluidKey) fluid).potion;
        } else if (fluid == FluidKeys.WATER) {
            potion = PotionContents.ofPotion(Potions.WATER);
        } else if (fluid == FluidKeys.EMPTY) {
            return new ItemStack(Items.GLASS_BOTTLE);
        } else {
            return ItemStack.EMPTY;
        }
        return potion.createStack(Items.POTION, 1);
    }

    @Override
    public FluidAmount libblockattributes__getFluidVolumeAmount() {
        return FluidAmount.BOTTLE;
    }
}
