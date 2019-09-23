/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.shape.VoxelShape;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.CacheInfo;
import alexiil.mc.lib.attributes.Convertible;
import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInvView;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.GroupedItemInvViewFixedWrapper;
import alexiil.mc.lib.attributes.item.impl.MappedFixedItemInvView;
import alexiil.mc.lib.attributes.item.impl.SubFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.SubFixedItemInvView;

/** A view of a fixed inventory for items, where the size of the inventory never changes, and every slot is "simple":
 * <ul>
 * <li>The amount of every slot will never exceed 64, the normal maximum stack size of minecraft.</li>
 * <li>The stack will stay in the slot until it is removed or changed by something else. (So setting the stack in a slot
 * of an {@link FixedItemInv} will reflect that change in {@link #getInvStack(int)}).</li>
 * </ul>
 * <p>
 * The attribute is stored in {@link ItemAttributes#FIXED_INV_VIEW}.
 * <p>
 * There are various classes of interest:
 * <ul>
 * <li>A modifiable version of this is {@link FixedItemInv}.</li>
 * <li>The null instance is {@link EmptyFixedItemInv}</li>
 * <li>A combined view of several sub-inventories is {@link CombinedFixedItemInvView}.</li>
 * <li>A partial view of a single inventory is {@link SubFixedItemInv}</li>
 * </ul>
 */
public interface FixedItemInvView extends Convertible, AbstractItemInvView {

    /** @return The number of slots in this inventory. */
    int getSlotCount();

    /** @param slot The slot index. Must be a value between 0 (inclusive) and {@link #getSlotCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return The ItemStack that is held in the inventory at the moment. It is unspecified whether you are allowed to
     *         modify this returned {@link ItemStack} - however subinterfaces (like {@link FixedItemInv}) may have
     *         different limitations on this. Note that this stack might not be valid for this slot in either
     *         {@link #isItemValidForSlot(int, ItemStack)} or {@link #getFilterForSlot(int)}.
     * @throws RuntimeException if the given slot wasn't a valid index. */
    ItemStack getInvStack(int slot);

    /** @param slot The slot index. Must be a value between 0 (inclusive) and {@link #getSlotCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @param stack The stack to check for. May be an {@link ItemStack#isEmpty() empty} stack to get the maximum amount
     *            that this can hold of any stack.
     * @return The maximum amount that the given slot can hold of the given stack. This method will ignore the current
     *         stack in {@link #getInvStack(int)}. The default implementation just delegates to
     *         {@link ItemStack#getMaxCount()}. Note that any setters that this object implements (like
     *         {@link FixedItemInv#setInvStack(int, ItemStack, Simulation)} should reject stacks that are greater than
     *         this value. (and callers should only call this if they need to check the amounts separately. Note that it
     *         is meaningless to return values greater than the maximum amount an item can be stacked to here, and
     *         callers are free to throw an exception if this is violated. (Basically huge single-slot inventories
     *         shouldn't implement this interface).
     * @throws RuntimeException if the given slot wasn't a valid index. */
    default int getMaxAmount(int slot, ItemStack stack) {
        return stack.isEmpty() ? 64 : stack.getMaxCount();
    }

    /** Checks to see if the given stack is valid for a given slot. This ignores any current stacks in the slot.
     * 
     * @param slot The slot index. Must be a value between 0 (inclusive) and {@link #getSlotCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @param stack The {@link ItemStack} to check.
     * @throws RuntimeException if the given slot wasn't a valid index. */
    boolean isItemValidForSlot(int slot, ItemStack stack);

    /** @param slot The slot index. Must be a value between 0 (inclusive) and {@link #getSlotCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return An {@link ItemFilter} for this slot. If this slot is filtered by an {@link ItemFilter} internally then it
     *         is highly recommended that this be overridden to return *that* filter rather than a newly constructed
     *         one.
     * @throws RuntimeException if the given slot wasn't a valid index. */
    default ItemFilter getFilterForSlot(int slot) {
        return stack -> isItemValidForSlot(slot, stack);
    }

    /** @return A view of a single slot in this inventory. */
    default SingleItemSlotView getSlot(int slot) {
        return new SingleItemSlotView(this, slot);
    }

    default Iterable<? extends SingleItemSlotView> slotIterable() {
        return () -> new Iterator<SingleItemSlotView>() {
            int index = 0;

            @Override
            public SingleItemSlotView next() {
                return getSlot(index++);
            }

            @Override
            public boolean hasNext() {
                return index < getSlotCount();
            }
        };
    }

    default Iterable<ItemStack> stackIterable() {
        return () -> new Iterator<ItemStack>() {
            int index = 0;

            @Override
            public ItemStack next() {
                return getInvStack(index++);
            }

            @Override
            public boolean hasNext() {
                return index < getSlotCount();
            }
        };
    }

    /** @return A {@link GroupedItemInvView} of this inventory. */
    default GroupedItemInvView getGroupedInv() {
        return new GroupedItemInvViewFixedWrapper(this);
    }

    /** Equivalent to {@link List#subList(int, int)}.
     * 
     * @param fromIndex The first slot to expose
     * @param toIndex The slot after the last slot to expose.
     * @return a view of this inventory that only exposes the given number of slots. Might return "this" if fromIndex is
     *         0 and toIndex is equal to {@link #getSlotCount()}.
     * @throws RuntimeException if any of the given slots weren't valid. */
    default FixedItemInvView getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }
        if (fromIndex == 0 && toIndex == getSlotCount()) {
            return this;
        }
        return new SubFixedItemInvView(this, fromIndex, toIndex);
    }

    /** @param slots The slots to expose.
     * @return a view of this inventory that only exposes the given slots. Might return "this" if the slot array is just
     *         [0,1, ... {@link #getSlotCount()}-1]
     * @throws RuntimeException if any of the given slots weren't valid. */
    default FixedItemInvView getMappedInv(int... slots) {
        if (slots.length == 0) {
            return EmptyFixedItemInv.INSTANCE;
        }
        if (areSlotArraysEqual(this, slots)) {
            return this;
        }
        return new MappedFixedItemInvView(this, slots);
    }

    /** Used as a helper for {@link #getMappedInv(int...)}, to see if it should return itself or not. */
    public static boolean areSlotArraysEqual(FixedItemInvView inv, int[] slots) {
        return isFlatSlotArray(slots, inv.getSlotCount());
    }

    /** @return True if the given array is equal to [0, 1, 2 ... count-2, count-1]. */
    public static boolean isFlatSlotArray(int[] slots, int count) {
        if (slots.length == count) {
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] != i) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /** Offers this object and {@link #getGroupedInv()} to the attribute list. (Which, in turn, adds
     * {@link FixedItemInv#getInsertable()}, {@link FixedItemInv#getExtractable()}, and
     * {@link FixedItemInv#getTransferable()} to the list as well).
     * 
     * @deprecated Because this functionality has been fully replaced by {@link Convertible} and it's usage in
     *             {@link AttributeList}, so you can always just offer this object directly to the attribute list. */
    @Deprecated
    default void offerSelfAsAttribute(AttributeList<?> list, @Nullable CacheInfo cacheInfo,
        @Nullable VoxelShape shape) {
        list.offer(this, cacheInfo, shape);
    }

    @Override
    default <T> T convertTo(Class<T> otherType) {
        return Convertible.offer(otherType, getGroupedInv());
    }

    /** @return An object that only implements {@link FixedItemInvView}, and does not expose the modification methods
     *         that {@link FixedItemInv} does. Implementations that don't expose any modification methods themselves
     *         should override this method to just return themselves. */
    default FixedItemInvView getFixedView() {
        final FixedItemInvView real = this;
        return new FixedItemInvView() {
            @Override
            public int getSlotCount() {
                return real.getSlotCount();
            }

            @Override
            public ItemStack getInvStack(int slot) {
                return real.getInvStack(slot);
            }

            @Override
            public boolean isItemValidForSlot(int slot, ItemStack item) {
                return real.isItemValidForSlot(slot, item);
            }

            @Override
            public int getMaxAmount(int slot, ItemStack stack) {
                return real.getMaxAmount(slot, stack);
            }

            @Override
            public ItemFilter getFilterForSlot(int slot) {
                return real.getFilterForSlot(slot);
            }

            @Override
            public GroupedItemInvView getGroupedInv() {
                return new GroupedItemInvViewFixedWrapper(this);
            }

            @Override
            public ListenerToken addListener(InvMarkDirtyListener listener, ListenerRemovalToken removalToken) {
                FixedItemInvView wrapper = this;
                return real.addListener(
                    (inv) -> {
                        // Defend against giving the listener the real (possibly changeable) inventory.
                        // In addition the listener would probably cache *this view* rather than the backing inventory
                        // so they most likely need it to be this inventory.
                        listener.onMarkDirty(wrapper);
                    }, removalToken
                );
            }

            @Override
            public int getChangeValue() {
                return real.getChangeValue();
            }
        };
    }
}
