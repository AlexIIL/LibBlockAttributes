/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat.mod.dank;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.AttributeSourceType;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.ItemAttributes;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper;

class DankItemInvCompat {

    static void load(Class<? extends BlockEntity> cls) {
        ItemAttributes.forEachInv(attribute -> {
            attribute.putBlockEntityClassAdder(AttributeSourceType.INSTANCE, cls, true, (be, to) -> {
                to.offer(new FixedInventoryVanillaWrapper((Inventory) be) {

                    @Override
                    public ItemStack insertStack(int slot, ItemStack stack, Simulation simulation) {
                        if (stack.isEmpty()) {
                            return ItemStack.EMPTY;
                        }
                        // Effectively a copy of FixedIteminv.insertStack, but ignores stack.getMaxCount()
                        // (and specialised a bit for the vanilla-like inventory)

                        ItemStack current = inv.getStack(slot);
                        if (!current.isEmpty() && !ItemStackUtil.areEqualIgnoreAmounts(stack, current)) {
                            return stack;
                        }
                        int max = inv.getMaxCountPerStack();
                        int space = max - current.getCount();
                        int addable = Math.min(stack.getCount(), space);
                        if (addable <= 0) {
                            return stack;
                        }
                        stack = stack.copy();
                        ItemStack newInSlot = current;
                        if (current.isEmpty()) {
                            newInSlot = stack.split(addable);
                        } else {
                            stack.split(addable);
                            if (simulation.isAction()) {
                                newInSlot.increment(addable);
                            }
                        }

                        if (simulation.isAction()) {
                            inv.setStack(slot, newInSlot);
                        }

                        return stack;
                    }
                });
            });
        });
    }

}
