/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;

public class MappedFixedItemInv extends MappedFixedItemInvView implements FixedItemInv {

    public MappedFixedItemInv(FixedItemInv inv, int[] slots) {
        super(inv, slots);
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return ((FixedItemInv) inv).setInvStack(getInternalSlot(slot), to, simulation);
    }
}
