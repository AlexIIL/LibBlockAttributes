/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.item;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil.FluidTransferResult;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilterUtil;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.AbstractItemBasedAttribute;
import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;

/** An abstract {@link GroupedFluidInv} for an {@link AbstractItemBasedAttribute} which only ever contains a single
 * fluid, and can contain any amount of said fluid between 0 and it's capacity. */
public abstract class ItemBasedSingleFluidInv extends AbstractItemBasedAttribute implements GroupedFluidInv {
    protected ItemBasedSingleFluidInv(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excessStacks) {
        super(stackRef, excessStacks);
    }

    /** @param stack The {@link ItemStack} to test. This will never be empty.
     * @return True if the given stack is not a valid target. */
    protected abstract boolean isInvalid(ItemStack stack);

    /** @param stack The {@link ItemStack} of which to look at. This will have already been passed to
     *            {@link #isInvalid(ItemStack)} and returned false. You should ignore the {@link ItemStack#getCount()},
     *            and treat it as 1.
     * @return The information for a given stack. */
    protected abstract HeldFluidInfo getInfo(ItemStack stack);

    /** @param stack The stack to modify. However it is not required that this stack is actually modified, instead you
     *            could create a new stack and return it.
     * @return Either the newly modified stack (if the given fluid was valid), or null if this couldn't write the given
     *         fluid to the stack. */
    @Nullable
    protected abstract ItemStack writeToStack(ItemStack stack, FluidVolume fluid);

    @Override
    public Set<FluidKey> getStoredFluids() {
        ItemStack stack = stackRef.get();
        if (stack.isEmpty() || isInvalid(stack)) {
            return Collections.emptySet();
        }
        HeldFluidInfo info = getInfo(stack);
        if (info.fluid.isEmpty()) {
            return Collections.emptySet();
        } else {
            return Collections.singleton(info.fluid.fluidKey);
        }
    }

    @Override
    public FluidInvStatistic getStatistics(FluidFilter filter) {
        ItemStack stack = stackRef.get();
        if (stack.isEmpty() || isInvalid(stack)) {
            return FluidInvStatistic.emptyOf(filter);
        }
        HeldFluidInfo info = getInfo(stack);
        if (!FluidFilterUtil.hasIntersection(filter, getInsertionFilter())) {
            return FluidInvStatistic.emptyOf(filter);
        }

        FluidAmount capacity = info.capacity;
        if (info.fluid.isEmpty()) {
            return new FluidInvStatistic(filter, FluidAmount.ZERO, capacity, capacity);
        } else {
            if (filter.matches(info.fluid.fluidKey)) {
                FluidAmount spaceAddable = capacity.sub(info.fluid.getAmount_F());
                return new FluidInvStatistic(filter, info.fluid.getAmount_F(), spaceAddable, capacity);
            } else {
                return new FluidInvStatistic(filter, FluidAmount.ZERO, FluidAmount.ZERO, capacity);
            }
        }
    }

    /** The {@link FluidFilter} to limit what may be inserted. NOTE: subclasses must <em>never</em> call
     * {@link #attemptInsertion(FluidVolume, Simulation)} from inside this method, or return a {@link FluidFilter} whose
     * {@link FluidFilter#matches(FluidKey) matches} method calls attemptInsertion as this is used by attemptInsertion
     * in order to filter incoming fluids. */
    @Override
    public FluidFilter getInsertionFilter() {
        return ConstantFluidFilter.ANYTHING;
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        if (fluid.isEmpty()) {
            return fluid;
        }
        if (!getInsertionFilter().matches(fluid.fluidKey)) {
            return fluid;
        }
        ItemStack stack = stackRef.get();
        if (stack.isEmpty() || isInvalid(stack)) {
            return fluid;
        }
        HeldFluidInfo info = getInfo(stack);
        FluidTransferResult result = FluidVolumeUtil.computeInsertion(info.fluid, info.capacity, fluid);
        if (result.inTank == info.fluid) {
            return fluid;
        }
        ItemStack oldStack = stack.copy();
        ItemStack newStack = oldStack.split(1);
        newStack = writeToStack(newStack, result.inTank);
        if (newStack == null) {
            return fluid;
        }
        if (setStacks(simulation, oldStack, newStack)) {
            return result.result;
        } else {
            return fluid;
        }
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        ItemStack stack = stackRef.get();
        if (stack.isEmpty() || isInvalid(stack)) {
            return FluidVolumeUtil.EMPTY;
        }
        HeldFluidInfo info = getInfo(stack);
        if (info.fluid.isEmpty()) {
            return FluidVolumeUtil.EMPTY;
        }
        FluidTransferResult result = FluidVolumeUtil.computeExtraction(info.fluid, filter, maxAmount);
        if (result.inTank == info.fluid) {
            return FluidVolumeUtil.EMPTY;
        }
        ItemStack oldStack = stack.copy();
        ItemStack newStack = oldStack.split(1);
        newStack = writeToStack(newStack, result.inTank);
        if (setStacks(simulation, oldStack, newStack)) {
            return result.result;
        } else {
            return FluidVolumeUtil.EMPTY;
        }
    }

    public static final class HeldFluidInfo {
        public final FluidVolume fluid;
        public final FluidAmount capacity;

        public HeldFluidInfo(FluidVolume fluid, FluidAmount capacity) {
            this.fluid = fluid;
            this.capacity = capacity;
        }
    }
}
