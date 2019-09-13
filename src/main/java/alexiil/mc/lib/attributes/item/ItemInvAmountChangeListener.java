/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

@FunctionalInterface
public interface ItemInvAmountChangeListener {

    /** @param inv The inventory that changed
     * @param stack The {@link ItemStack} whose amount changed.
     * @param previous The previous amount of the given stack.
     * @param current The new amount of the given stack. */
    void onChange(GroupedItemInvView inv, ItemStack stack, int previous, int current);

    /** A simple listener for an {@link ItemInvAmountChangeListener} that wraps an {@link InvMarkDirtyListener}. */
    public static final class MarkDirtyWrapper implements ItemInvAmountChangeListener {

        public final InvMarkDirtyListener realListener;

        public MarkDirtyWrapper(InvMarkDirtyListener realListener) {
            this.realListener = realListener;
        }

        @Override
        public void onChange(GroupedItemInvView inv, ItemStack stack, int previous, int current) {
            realListener.onMarkDirty(inv);
        }
    }
}
