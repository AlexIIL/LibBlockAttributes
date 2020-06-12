/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat.mod.emi.iteminv;

import net.minecraft.item.ItemStack;

import dev.emi.iteminventory.api.ItemInventory;

public enum EmiEmptyItemInventory implements ItemInventory {
    INSTANCE;

    @Override
    public int getInvSize(ItemStack invItem) {
        return 0;
    }

    @Override
    public ItemStack getStack(ItemStack invItem, int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void setStack(ItemStack invItem, int index, ItemStack stack) {
        throw new IndexOutOfBoundsException();
    }
}
