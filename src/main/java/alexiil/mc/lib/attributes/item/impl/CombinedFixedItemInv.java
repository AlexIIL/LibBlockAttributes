/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;

/** An {@link FixedItemInv} that delegates to a list of them instead of storing items directly. */
public class CombinedFixedItemInv<InvType extends FixedItemInv> extends CombinedFixedItemInvView<InvType>
    implements FixedItemInv {

    public CombinedFixedItemInv(List<? extends InvType> views) {
        super(views);
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return getInv(slot).setInvStack(getSubSlot(slot), to, simulation);
    }
}
