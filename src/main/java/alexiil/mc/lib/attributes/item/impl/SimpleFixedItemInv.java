/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.FixedItemInv.CopyingFixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInv.ModifiableFixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;

/** @deprecated You should use either {@link DirectFixedItemInv} or {@link FullFixedItemInv} instead of this!
 *             <p>
 *             Full explanation: As of LBA 0.5.0 there's been a large change to how inventories work to be much closer
 *             to vanilla - inventories no longer return immutable stacks from
 *             {@link FixedItemInvView#getInvStack(int)}, and 2 new sub-interfaces have been created to handle the
 *             different ways of using this, either {@link ModifiableFixedItemInv} (which exposes a markDirty() method
 *             just like vanilla {@link Inventory} does), or {@link CopyingFixedItemInv} (which returns copies of the
 *             internal {@link ItemStack} and supports complete filtering and listener capabilities).
 *             <p>
 *             You should replace this class with either:
 *             <ul>
 *             <li>{@link FullFixedItemInv} if you either need exact per-slot listening or the full filtration
 *             capabilities</li>
 *             <li>{@link DirectFixedItemInv} if you'd rather use a more vanilla oriented approach of being able to
 *             directly modify items in an inventory.</li>
 *             </ul>
 */
@Deprecated
public class SimpleFixedItemInv extends FullFixedItemInv {

    // Copy-pasted from the top
    /** @deprecated You should use either {@link DirectFixedItemInv} or {@link FullFixedItemInv} instead of this!
     *             <p>
     *             Full explanation: As of LBA 0.5.0 there's been a large change to how inventories work to be much
     *             closer to vanilla - inventories no longer return immutable stacks from
     *             {@link FixedItemInvView#getInvStack(int)}, and 2 new sub-interfaces have been created to handle the
     *             different ways of using this, either {@link ModifiableFixedItemInv} (which exposes a markDirty()
     *             method just like vanilla {@link Inventory} does), or {@link CopyingFixedItemInv} (which returns
     *             copies of the internal {@link ItemStack} and supports complete filtering and listener capabilities).
     *             <p>
     *             You should replace this class with either:
     *             <ul>
     *             <li>{@link FullFixedItemInv} if you either need exact per-slot listening or the full filtration
     *             capabilities</li>
     *             <li>{@link DirectFixedItemInv} if you'd rather use a more vanilla oriented approach of being able to
     *             directly modify items in an inventory.</li>
     *             </ul>
     */
    @Deprecated
    public SimpleFixedItemInv(int invSize) {
        super(invSize);
    }
}
