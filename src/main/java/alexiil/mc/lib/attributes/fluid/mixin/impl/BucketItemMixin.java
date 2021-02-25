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
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.registry.Registry;

import alexiil.mc.lib.attributes.fluid.FluidProviderItem;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.ICustomBucketItem;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.mixin.api.IBucketItem;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes;
import alexiil.mc.lib.attributes.misc.Ref;

@Mixin(BucketItem.class)
public class BucketItemMixin extends Item implements FluidProviderItem, IBucketItem {

    @Final
    @Shadow
    private Fluid fluid;

    private boolean logged_nonVanillaButNotCustom;

    public BucketItemMixin(Item.Settings settings) {
        super(settings);
    }

    @Override
    public FluidVolume drain(Ref<ItemStack> stack) {
        if (fluid == Fluids.EMPTY || ((Object) this) instanceof EntityBucketItem) {
            return FluidVolumeUtil.EMPTY;
        }

        Item remainder = this.getRecipeRemainder();
        FluidKey fluidKey = FluidKeys.get(fluid);
        if (remainder == null || fluidKey == null) {
            return FluidVolumeUtil.EMPTY;
        }
        stack.obj = new ItemStack(remainder);
        return fluidKey.withAmount(FluidAmount.BUCKET);
    }

    @Override
    public boolean fill(Ref<ItemStack> stack, Ref<FluidVolume> with) {
        if (fluid != Fluids.EMPTY) {
            return false;
        }
        for (Item item : Registry.ITEM) {
            if (!(item instanceof FluidProviderItem)) {
                continue;
            }
            FluidProviderItem bucket = (FluidProviderItem) item;
            ItemStack newStack = new ItemStack(item);

            Ref<ItemStack> stackRef = new Ref<>(newStack);
            FluidVolume fluidHeld = bucket.drain(stackRef);
            FluidAmount amount = fluidHeld.getAmount_F();
            if (!FluidVolume.areEqualExceptAmounts(with.obj, fluidHeld)) {
                continue;
            }
            if (amount.isGreaterThan(with.obj.getAmount_F())) {
                continue;
            }
            if (!ItemStack.areEqual(stackRef.obj, stack.obj)) {
                continue;
            }
            with.obj = with.obj.copy();
            FluidVolume splitOff = with.obj.split(amount);
            assert splitOff.getAmount_F().equals(amount);
            stack.obj = newStack;
            return true;
        }
        return false;
    }

    @Override
    public boolean libblockattributes__shouldExposeFluid() {
        return !(((Object) this) instanceof EntityBucketItem);
    }

    @Override
    public FluidKey libblockattributes__getFluid(ItemStack stack) {
        return FluidKeys.get(fluid);
    }

    @Override
    public ItemStack libblockattributes__withFluid(FluidKey fluid) {
        if (fluid == FluidKeys.EMPTY) {
            return new ItemStack(getRecipeRemainder());
        }
        Fluid rawFluid = fluid.getRawFluid();
        if (rawFluid == null) {
            return ItemStack.EMPTY;
        }
        if (this instanceof ICustomBucketItem) {
            return ((ICustomBucketItem) this).getFilledBucket(rawFluid);
        } else if (this != Items.BUCKET) {
            if (!logged_nonVanillaButNotCustom) {
                logged_nonVanillaButNotCustom = true;
                LibBlockAttributes.LOGGER.warn("Unknown non-vanilla BucketItem " + Registry.ITEM.getId(this));
            }
            return ItemStack.EMPTY;
        }
        return new ItemStack(rawFluid.getBucketItem());
    }

    @Override
    public FluidAmount libblockattributes__getFluidVolumeAmount() {
        return FluidAmount.BUCKET;
    }
}
