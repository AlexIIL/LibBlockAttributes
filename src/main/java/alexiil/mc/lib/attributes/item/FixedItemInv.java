/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import java.util.function.Function;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.GroupedItemInvFixedWrapper;
import alexiil.mc.lib.attributes.item.impl.ItemInvModificationTracker;
import alexiil.mc.lib.attributes.item.impl.MappedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.SimpleLimitedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.SubFixedItemInv;

/** A {@link FixedItemInvView} that can have it's contents changed. Note that this does not imply that the contents can
 * be changed to anything the caller wishes them to be, as implementations can limit the valid {@link ItemStack}s
 * allowed.
 * <p>
 * The attribute is stored in {@link ItemAttributes#FIXED_INV}.
 * <p>
 * <p>
 * There are various classes of interest:
 * <ul>
 * <li>The null instance is {@link EmptyFixedItemInv}</li>
 * <li>A combined view of several sub-inventories is {@link CombinedFixedItemInv}.</li>
 * </ul>
 * It is <em>highly</em> recommended that implementations always extend either {@link CopyingFixedItemInv} <em>or</em>
 * {@link ModifiableFixedItemInv}. (One of the two, but never both - consumers who receive a {@link FixedItemInv} should
 * feel free to throw an exception if it implements both, since it makes no sense. However it is always permitted to
 * implement neither). */
public interface FixedItemInv extends FixedItemInvView {

    /** @return A modifiable version of the {@link ItemStack} that is stored in this inventory. Note that this *may* be
     *         a {@link ItemStack#copy() copy}: changing the returned {@link ItemStack} might not change the next
     *         returned stack. */
    @Override
    ItemStack getInvStack(int slot);

    /** Sets the stack in the given slot to the given stack.
     * 
     * @param to The new {@link ItemStack}. It is not defined if you are allowed to modify this or not.
     * @return True if the modification was allowed, false otherwise. (For example if the given stack doesn't pass the
     *         {@link FixedItemInvView#isItemValidForSlot(int, ItemStack)} test). */
    boolean setInvStack(int slot, ItemStack to, Simulation simulation);

    /** Sets the stack in the given slot to the given stack, or throws an exception if it was not permitted. */
    default void forceSetInvStack(int slot, ItemStack to) {
        if (!setInvStack(slot, to, Simulation.ACTION)) {
            throw new IllegalStateException(
                "Unable to force-set the slot " + slot + " to " + ItemInvModificationTracker.stackToFullString(to) + "!"
            );
        }
    }

    /** Applies the given function to the stack held in the slot, and uses {@link #forceSetInvStack(int, ItemStack)} on
     * the result (Which will throw an exception if the returned stack is not valid for this inventory). */
    default void modifySlot(int slot, Function<ItemStack, ItemStack> function) {
        forceSetInvStack(slot, function.apply(getInvStack(slot)));
    }

    @Override
    default SingleItemSlot getSlot(int slot) {
        return new SingleItemSlot(this, slot);
    }

    /** @return A new {@link LimitedFixedItemInv} that provides a more controllable version of this
     *         {@link FixedItemInv}. */
    default LimitedFixedItemInv createLimitedFixedInv() {
        return SimpleLimitedFixedItemInv.createLimited(this);
    }

    /* Although getGroupedItemInv() makes get{Insertable,Extractable,Transferable} all redundant, it's quite helpful to
     * be able to call the method name that matches what you want to do with it. */

    /** @return An {@link ItemInsertable} for this inventory that will attempt to insert into any of the slots in this
     *         inventory. The default implementation delegates to {@link #getGroupedInv()}. */
    default ItemInsertable getInsertable() {
        return getGroupedInv();
    }

    /** @return An {@link ItemExtractable} for this inventory that will attempt to extract from any of the slots in this
     *         inventory. The default implementation delegates to {@link #getGroupedInv()}. */
    default ItemExtractable getExtractable() {
        return getGroupedInv();
    }

    /** @return An {@link ItemTransferable} for this inventory. The default implementation delegates to
     *         {@link #getGroupedInv()}. */
    default ItemTransferable getTransferable() {
        return getGroupedInv();
    }

    /** @return A {@link GroupedItemInv} for this inventory. The returned value must always be valid for the lifetime of
     *         this {@link FixedItemInv} object. (In other words it must always be valid to cache this returned value
     *         and use it alongside a cached instance of this object). */
    @Override
    default GroupedItemInv getGroupedInv() {
        return new GroupedItemInvFixedWrapper(this);
    }

    @Override
    default FixedItemInv getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }
        if (fromIndex == 0 && toIndex == getSlotCount()) {
            return this;
        }
        return SubFixedItemInv.create(this, fromIndex, toIndex);
    }

    @Override
    default FixedItemInv getMappedInv(int... slots) {
        if (slots.length == 0) {
            return EmptyFixedItemInv.INSTANCE;
        }
        if (slots.length == getSlotCount()) {
            boolean isThis = true;
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] != i) {
                    isThis = false;
                    break;
                }
            }
            if (isThis) {
                return this;
            }
        }
        return MappedFixedItemInv.create(this, slots);
    }

    /** The "simpler" variant of {@link FixedItemInv} which allows callers to freely modify the current
     * {@link ItemStack} contained in it's inventory. */
    public interface ModifiableFixedItemInv extends FixedItemInv {

        /** @return The {@link ItemStack} that is stored in this {@link FixedItemInv}. Changing this will
         *         <em>always</em> change this inventory. As such you must always call {@link #markDirty()} or
         *         {@link #setInvStack(int, ItemStack, Simulation)} after you have finished modifying it. */
        @Override
        ItemStack getInvStack(int slot);

        /** Checks to see if the given stack is valid for a given slot. This ignores any current stacks in the slot.
         * Note that this should only compare the {@link Item} contained in {@link ItemStack}'s, because callers can
         * always modify any other properties (like count or NBT) themselves */
        @Override
        boolean isItemValidForSlot(int slot, ItemStack stack);

        /** Note that this filter should only compare the {@link Item} contained in {@link ItemStack}'s, because callers
         * can always modify any other properties (like count or NBT) themselves. */
        @Override
        default ItemFilter getFilterForSlot(int slot) {
            return FixedItemInv.super.getFilterForSlot(slot);
        }

        /** @param to The new stack to set this to. If this is identically equal (with ==) to the stack held in this
         *            inventory (so it was returned by {@link #getInvStack(int)}) then this will return true. */
        @Override
        boolean setInvStack(int slot, ItemStack to, Simulation simulation);

        /** Informs this inventory that the {@link ItemStack} returned by {@link #getInvStack(int)} has been changed. */
        void markDirty();
    }

    /** The "complex" variant of {@link FixedItemInv} that always returns copies of the stack held. As such this allows
     * per-slot listeners to be registered, and full filter usage (as no-one can modify this inventory in a way that is
     * not permitted). */
    public interface CopyingFixedItemInv extends FixedItemInv {

        /** @return a copy of the {@link ItemStack} held in this inventory. */
        @Override
        default ItemStack getInvStack(int slot) {
            return getUnmodifiableInvStack(slot).copy();
        }

        /** @return The {@link ItemStack} that is held by this inventory. Modifying the returned {@link ItemStack} in
         *         any way will (most likely - depending on the implementation) throw an exception (at some point).
         *         <p>
         *         Some implementations */
        ItemStack getUnmodifiableInvStack(int slot);

        @Override
        default SingleCopyingItemSlot getSlot(int slot) {
            return new SingleCopyingItemSlot(this, slot);
        }

        @Override
        default ListenerToken addListener(InvMarkDirtyListener listener, ListenerRemovalToken removalToken) {
            FixedItemInv wrapper = this;
            return addListener((inv, slot, before, after) -> {
                listener.onMarkDirty(wrapper);
            }, removalToken);
        }

        /** Adds the given listener to this inventory, such that the
         * {@link ItemInvSlotChangeListener#onChange(FixedItemInvView, int, ItemStack, ItemStack)} will be called every
         * time that this inventory changes. However if this inventory doesn't support listeners then this will return a
         * null {@link ListenerToken token}.
         * 
         * @param removalToken A token that will be called whenever the given listener is removed from this inventory
         *            (or if this inventory itself is unloaded or otherwise invalidated).
         * @return A token that represents the listener, or null if the listener could not be added. */
        ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken);
    }
}
