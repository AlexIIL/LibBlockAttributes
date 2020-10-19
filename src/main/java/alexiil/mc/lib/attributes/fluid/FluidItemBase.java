/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.util.Set;

import net.minecraft.item.FishBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInvView.FluidInvStatistic;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilterUtil;
import alexiil.mc.lib.attributes.fluid.mixin.api.IBucketItem;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.AbstractItemBasedAttribute;
import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;

/** Base class for {@link FluidAttributes.BucketItemGroupedFluidInv} and
 * {@link FluidContainerRegistry.StateEmpty.EmptyBucketInv} to handle {@link IBucketItem}. */
/* sealed */ abstract class FluidItemBase extends AbstractItemBasedAttribute
// permits FluidAttributes.BucketItemGroupedFluidInv, FluidContainerRegistry.StateEmpty.EmptyBucketInv
{
    protected FluidItemBase(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excessStacks) {
        super(stackRef, excessStacks);
    }

    static boolean isIBucket(ItemStack stack) {
        return isIBucket(stack.getItem());
    }

    static boolean isIBucket(Item item) {
        if (item instanceof FishBucketItem) {
            return false;
        }
        return item instanceof IBucketItem && ((IBucketItem) item).libblockattributes__shouldExposeFluid();
    }

    FluidInvStatistic getIBucketStatistics(ItemStack stack, FluidFilter filter) {
        if (!isIBucket(stack)) {
            return FluidInvStatistic.emptyOf(filter);
        }

        IBucketItem bucket = (IBucketItem) stack.getItem();
        FluidKey current = bucket.libblockattributes__getFluid(stack);

        if (current != FluidKeys.EMPTY) {
            if (filter.matches(current)) {
                FluidAmount perBucket = bucket.libblockattributes__getFluidVolumeAmount();
                FluidAmount amount = perBucket.checkedMul(stack.getCount());
                return new FluidInvStatistic(filter, amount, FluidAmount.ZERO, amount);
            } else {
                return FluidInvStatistic.emptyOf(filter);
            }
        }

        Set<FluidKey> any = FluidFilterUtil.decomposeFilter(filter);

        if (any != null) {
            FluidAmount perBucket = bucket.libblockattributes__getFluidVolumeAmount();
            FluidAmount space = perBucket.checkedMul(stack.getCount());
            for (FluidKey key : any) {
                if (!bucket.libblockattributes__withFluid(key).isEmpty()) {
                    return new FluidInvStatistic(filter, FluidAmount.ZERO, FluidAmount.ZERO, space);
                }
            }
        }

        return FluidInvStatistic.emptyOf(filter);
    }

    FluidVolume attemptIBucketInsertion(ItemStack stack, FluidVolume fluid, Simulation simulation) {
        if (!isIBucket(stack)) {
            return fluid;
        }
        IBucketItem bucket = (IBucketItem) stack.getItem();
        FluidAmount perBucket = bucket.libblockattributes__getFluidVolumeAmount();
        if (fluid.getAmount_F().isLessThan(perBucket)) {
            return fluid;
        }
        FluidKey current = bucket.libblockattributes__getFluid(stack);
        if (!current.isEmpty()) {
            return fluid;
        }
        ItemStack newStack = bucket.libblockattributes__withFluid(fluid.fluidKey);
        if (newStack.isEmpty()) {
            return fluid;
        }

        stack = stack.copy();
        stack.decrement(1);

        FluidVolume originalFluid = fluid;
        fluid = fluid.copy();
        FluidVolume splitOff = fluid.split(perBucket);
        if (!splitOff.getAmount_F().equals(perBucket)) {
            throw new IllegalStateException(
                "Split off amount was not equal to perBucket!"//
                    + "\n\tsplitOff = " + splitOff//
                    + "\n\tfluid = " + fluid//
                    + "\n\tperBucket = " + perBucket//
            );
        }

        return setStacks(simulation, stack, newStack) ? fluid : originalFluid;

    }
}
