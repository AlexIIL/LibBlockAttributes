/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.impl.DirectFixedItemInv;

/** Listener {@link FunctionalInterface} for {@link FixedItemInvView}.
 * <p>
 * Note that the listener system is not fully fleshed out yet so this <em>will</em> change in the future! */
@FunctionalInterface
public interface ItemInvSlotChangeListener {

    /** @param inv The inventory that changed
     * @param slot The slot that changed
     * @param previous The previous {@link ItemStack}.
     * @param current The new {@link ItemStack} */
    void onChange(FixedItemInvView inv, int slot, ItemStack previous, ItemStack current);

    /** A specialised type of listener that won't receive the previous {@link ItemStack} that occupied the given slot.
     * Used for optimisation purposes in {@link DirectFixedItemInv}. */
    @FunctionalInterface
    public interface ItemInvSlotListener extends ItemInvSlotChangeListener {

        /** NOTE: This might not be called if the inventory calls {@link #onChange(FixedItemInvView, int)} directly! */
        @Override
        default void onChange(FixedItemInvView inv, int slot, ItemStack previous, ItemStack current) {
            onChange(inv, slot);
        }

        void onChange(FixedItemInvView inv, int slot);
    }
}
