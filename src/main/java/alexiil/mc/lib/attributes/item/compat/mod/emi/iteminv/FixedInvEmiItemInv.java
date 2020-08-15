/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat.mod.emi.iteminv;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.ItemAttributes;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.misc.AbstractItemBasedAttribute;
import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;

import dev.emi.iteminventory.api.ItemInventory;

/** A {@link FixedItemInv} which wraps emi's {@link ItemInventory}.
 * <p>
 * Very few mods will need to use this directly - instead this should be obtained as an attribute through
 * {@link ItemAttributes#FIXED_INV} or similar. */
public class FixedInvEmiItemInv extends AbstractItemBasedAttribute implements FixedItemInv {

    public FixedInvEmiItemInv(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excessStacks) {
        super(stackRef, excessStacks);
    }

    protected static ItemInventory inv(ItemStack stack) {
        if (stack.getItem() instanceof ItemInventory) {
            return (ItemInventory) stack.getItem();
        }
        return EmiEmptyItemInventory.INSTANCE;
    }

    @Override
    public int getSlotCount() {
        ItemStack stack = stackRef.get();
        return inv(stack).getInvSize(stack);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack offered) {
        ItemStack stack = stackRef.get();
        ItemInventory inv = inv(stack);
        return inv.canInsert(stack, slot, offered);
    }

    @Override
    public ItemStack getInvStack(int slot) {
        ItemStack stack = stackRef.get();
        ItemInventory inv = inv(stack);
        return inv.getStack(stack, slot);
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        ItemStack invStack = stackRef.get();
        ItemInventory inv = inv(invStack);
        ItemStack current = inv.getStack(invStack, slot);
        boolean allowed = false;

        if (to.isEmpty()) {
            allowed = inv.canTake(invStack, slot);
        } else {
            if (current.isEmpty()) {
                allowed = inv.canInsert(invStack, slot, to);
            } else if (ItemStackUtil.areEqualIgnoreAmounts(to, current)) {
                if (to.getCount() < current.getCount()) {
                    allowed = inv.canTake(invStack, slot);
                } else {
                    allowed = inv.canInsert(invStack, slot, to);
                }
            } else {
                allowed = inv.canInsert(invStack, slot, to) && inv.canTake(invStack, slot);
            }
        }
        if (allowed) {
            ItemStack oldStack = invStack.copy();
            ItemStack newStack = oldStack.split(1);
            ((ItemInventory) newStack.getItem()).setStack(newStack, slot, to);
            return setStacks(simulation, oldStack, newStack);
        }
        return false;
    }
}
