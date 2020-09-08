/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;

/** Skeleton class for use when exposing an attribute (like a fluid inventory) from an item that is contained in a
 * reference. */
public abstract class AbstractItemBasedAttribute {

    protected final Reference<ItemStack> stackRef;
    protected final LimitedConsumer<ItemStack> excessStacks;

    protected AbstractItemBasedAttribute(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excessStacks) {
        this.stackRef = stackRef;
        this.excessStacks = excessStacks;
    }

    /** Attempts to place the stacks in the reference and excess.
     * 
     * @param oldStack A copied stack from {@link #stackRef}, but decreased by 1.
     * @param newStack The modified stack that was split off from {@link #stackRef}. */
    public boolean setStacks(Simulation simulation, ItemStack oldStack, ItemStack newStack) {
        if (oldStack.isEmpty() && stackRef.set(newStack, simulation)) {
            return true;
        } else if (stackRef.isValid(oldStack) && excessStacks.offer(newStack, simulation)) {
            boolean did = stackRef.set(oldStack, simulation);
            if (!did) {
                throw new IllegalStateException(
                    "Failed to set the stack! (Even though we just checked this up above...)" //
                        + "\n\tstackRef = " + stackRef + "\n\tstack = " + oldStack
                );
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + " in " + stackRef;
    }
}
