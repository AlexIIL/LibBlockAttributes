/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.mixin.impl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FishBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.registry.Registry;

import alexiil.mc.lib.attributes.fluid.FluidProviderItem;
import alexiil.mc.lib.attributes.fluid.mixin.api.IBucketItem;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.Ref;

@Mixin(BucketItem.class)
public class BucketItemMixin extends Item implements FluidProviderItem, IBucketItem {

    @Final
    @Shadow
    private Fluid fluid;

    public BucketItemMixin(Item.Settings settings) {
        super(settings);
    }

    @Override
    public FluidVolume drain(Ref<ItemStack> stack) {
        if (fluid == Fluids.EMPTY || ((Object) this) instanceof FishBucketItem) {
            return FluidKeys.EMPTY.withAmount(0);
        }

        Item remainder = this.getRecipeRemainder();
        FluidKey fluidKey = FluidKeys.get(fluid);
        if (remainder == null || fluidKey == null) {
            return FluidKeys.EMPTY.withAmount(0);
        }
        stack.obj = new ItemStack(remainder);
        return fluidKey.withAmount(FluidVolume.BUCKET);
    }

    @Override
    public boolean fill(Ref<ItemStack> stack, Ref<FluidVolume> with) {
        if (fluid != Fluids.EMPTY) {
            return false;
        }
        for (Item item : Registry.ITEM) {
            if (item instanceof FluidProviderItem) {
                FluidProviderItem bucket = (FluidProviderItem) item;
                ItemStack newStack = new ItemStack(item);

                Ref<ItemStack> stackRef = new Ref<>(newStack);
                FluidVolume fluidHeld = bucket.drain(stackRef);
                int amount = fluidHeld.getAmount();
                if (
                    FluidVolume.areEqualExceptAmounts(with.obj, fluidHeld) && amount <= with.obj.getAmount()
                        && ItemStack.areEqualIgnoreDamage(stackRef.obj, stack.obj)
                ) {
                    with.obj = with.obj.copy();
                    FluidVolume splitOff = with.obj.split(amount);
                    assert splitOff.getAmount() == amount;
                    stack.obj = newStack;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean libblockattributes__shouldExposeFluid() {
        return !(((Object) this) instanceof FishBucketItem);
    }

    @Override
    public FluidKey libblockattributes__getFluid(ItemStack stack) {
        return FluidKeys.get(fluid);
    }

    @Override
    public ItemStack libblockattributes__withFluid(FluidKey fluid) {
        // TODO: handle other (modded) bucket types? (Like wooden or steel or etc)
        if (fluid == FluidKeys.EMPTY) {
            return new ItemStack(Items.BUCKET);
        }
        Fluid rawFluid = fluid.getRawFluid();
        if (rawFluid == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(rawFluid.getBucketItem());
    }

    @Override
    public int libblockattributes__getFluidVolumeAmount() {
        return FluidVolume.BUCKET;
    }
}
