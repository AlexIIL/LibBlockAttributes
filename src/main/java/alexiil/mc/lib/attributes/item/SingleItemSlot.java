/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import java.util.function.Function;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemInsertableFilter;
import alexiil.mc.lib.attributes.misc.StackReference;

/** A delegating accessor of a single slot in a {@link FixedItemInv}. */
public class SingleItemSlot extends SingleItemSlotView implements ItemTransferable, StackReference {

    SingleItemSlot(FixedItemInv backingView, int slot) {
        super(backingView, slot);
    }

    @Override
    public FixedItemInv getBackingInv() {
        return (FixedItemInv) this.backingView;
    }

    /** Sets the stack in this slot to the given stack.
     * 
     * @return True if the modification was allowed, false otherwise. (For example if the given stack doesn't pass the
     *         FixedItemInvView.isItemValidForSlot(int, ItemStack) test). */
    @Override
    public final boolean set(ItemStack to, Simulation simulation) {
        return getBackingInv().setInvStack(slot, to, simulation);
    }

    /** Sets the stack in the given slot to the given stack, or throws an exception if it was not permitted. */
    public final void forceSet(ItemStack to) {
        getBackingInv().forceSetInvStack(slot, to);
    }

    /** Applies the given function to the stack held in the slot, and uses {@link #forceSet(ItemStack)} on the result
     * (Which will throw an exception if the returned stack is not valid for this inventory). */
    public final void modify(Function<ItemStack, ItemStack> function) {
        getBackingInv().modifySlot(slot, function);
    }

    @Override
    public final ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        return getBackingInv().extractStack(slot, filter, ItemStack.EMPTY, maxAmount, simulation);
    }

    @Override
    public final ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return getBackingInv().insertStack(slot, stack, simulation);
    }

    @Override
    public final ItemFilter getInsertionFilter() {
        return getBackingInv().getFilterForSlot(slot).and(new ItemInsertableFilter(this));
    }

    // Reference

    @Override
    public boolean set(ItemStack value) {
        return this.set(value, Simulation.ACTION);
    }
}
