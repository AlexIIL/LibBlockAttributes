/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;

/** An {@link FixedItemInv} that wraps a vanilla {@link Inventory}. */
public class FixedInventoryViewVanillaWrapper implements FixedItemInvView {
    protected final Inventory inv;

    public FixedInventoryViewVanillaWrapper(Inventory inv) {
        this.inv = inv;
    }

    public static FixedInventoryViewVanillaWrapper wrapInventory(Inventory inv) {
        return new FixedInventoryViewVanillaWrapper(inv);
    }

    @Override
    public int getSlotCount() {
        return inv.size();
    }

    @Override
    public ItemStack getInvStack(int slot) {
        return inv.getStack(slot);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return inv.isValid(slot, item);
    }

    @Override
    public int getMaxAmount(int slot, ItemStack stack) {
        return Math.min(inv.getMaxCountPerStack(), stack.getMaxCount());
    }
}
