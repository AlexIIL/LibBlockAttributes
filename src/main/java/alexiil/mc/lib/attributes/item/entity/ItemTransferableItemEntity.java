/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.entity;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.ItemTransferable;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

public class ItemTransferableItemEntity implements ItemTransferable {
    private final ItemEntity entity;

    public ItemTransferableItemEntity(ItemEntity entity) {
        this.entity = entity;
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        if (!entity.isAlive()) {
            return stack;
        }
        ItemStack current = entity.getStack();
        int max = current.getMaxCount() - current.getCount();
        if (max <= 0 || current.isEmpty()) {
            return stack;
        }
        if (!ItemStackUtil.areEqualIgnoreAmounts(stack, current)) {
            return stack;
        }
        stack = stack.copy();
        ItemStack insertable = stack.split(max);
        if (simulation == Simulation.ACTION) {
            current = current.copy();
            current.increment(insertable.getCount());
            entity.setStack(current);
        }
        return stack;
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        if (maxAmount < 1 || !entity.isAlive()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = entity.getStack();
        if (current.isEmpty() || !filter.matches(current)) {
            return ItemStack.EMPTY;
        }
        current = current.copy();
        ItemStack extracted = current.split(maxAmount);
        if (simulation == Simulation.ACTION) {
            entity.setStack(current);
            if (current.isEmpty()) {
                entity.remove();
            }
        }
        return extracted;
    }
}
