/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import java.util.Collections;
import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ExactItemStackFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

import it.unimi.dsi.fastutil.Hash.Strategy;

/** An alternative way of storing large numbers of {@link ItemStack}, without using large numbers of slots in a
 * {@link FixedItemInvView}. Instead of storing items in a List&lt;ItemStack&gt; this works more like a
 * Map&lt;ItemStack, int amount&gt;. */
public interface GroupedItemInvView extends AbstractItemInvView {

    /** @return a set containing all of the {@link ItemStack}'s that are stored in the inventory. NOTE: This must return
     *         a set using one of the {@link Strategy}'s in {@link ItemStackCollections} otherwise comparison methods
     *         won't work correctly! */
    Set<ItemStack> getStoredStacks();

    /** @param stack The stack to check for. Cannot be {@link ItemStack#isEmpty() empty}.
     * @return The total amount of the given stack that is stored in this inventory. */
    default int getAmount(ItemStack stack) {
        ItemInvStatistic stats = getStatistics(new ExactItemStackFilter(stack));
        assert stats.spaceTotal >= 0;
        return stats.amount;
    }

    /** @param stack The stack to check for. Must not be {@link ItemStack#isEmpty() empty}.
     * @return The total space that is available (right now!) to store the given stack. */
    default int getCapacity(ItemStack stack) {
        ItemInvStatistic stats = getStatistics(new ExactItemStackFilter(stack));
        assert stats.spaceTotal >= 0;
        return stats.amount + stats.spaceAddable + stats.spaceTotal;
    }

    /** @return The total capacity for every {@link ItemStack} that can be stored in this inventory.
     *         <p>
     *         NOTE: This value might <i>not</i> be equal to the sum of {@link #getCapacity(ItemStack)} over
     *         {@link #getStoredStacks()}! */
    int getTotalCapacity();

    /** @return The total space that could store the given stack, not including space that is currently taken up by the
     *         stack (so this should change with {@link #getAmount(ItemFilter)}). */
    default int getSpace(ItemStack stack) {
        return getCapacity(stack) - getAmount(stack);
    }

    /** @param filter The filter to check on.
     * @return Statistics about the currently stored amount, capacity, and space for everything that matches the given
     *         filter. */
    ItemInvStatistic getStatistics(ItemFilter filter);

    /** @param stack The {@link ItemStack} to check for.
     * @return Statistics about the currently stored amount, capacity, and space for everything that matches the given
     *         item stack. */
    default ItemInvStatistic getStatistics(ItemStack stack) {
        return getStatistics(new ExactItemStackFilter(stack));
    }

    /** @return A count of all the {@link ItemStack}'s that match the given filter. */
    default int getAmount(ItemFilter filter) {
        return getStatistics(filter).amount;
    }

    /** @return True if {@link #getAmount(ItemFilter) getAmount}(ConstantItemFilter.ANYTHING) returns a value greater
     *         than 0. */
    default boolean isEmpty() {
        return getAmount(ConstantItemFilter.ANYTHING) > 0;
    }

    @Override
    default ListenerToken addListener(InvMarkDirtyListener listener, ListenerRemovalToken removalToken) {
        return addListener(new ItemInvAmountChangeListener.MarkDirtyWrapper(listener), removalToken);
    }

    /** Adds the given listener to this inventory, such that the
     * {@link ItemInvAmountChangeListener#onChange(GroupedItemInvView, ItemStack, int, int)} will be called every time
     * that this inventory changes. However if this inventory doesn't support listeners then this will return a null
     * {@link ListenerToken token}.
     * <p>
     * The default implementation refuses to accept any listeners, but implementations are <em>highly encouraged</em> to
     * override this if they are able to!
     * 
     * @param removalToken A token that will be called whenever the given listener is removed from this inventory (or if
     *            this inventory itself is unloaded or otherwise invalidated).
     * @return A token that represents the listener, or null if the listener could not be added. */
    default ListenerToken addListener(ItemInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
        return null;
    }

    /** @return A completely unmodifiable view of this {@link GroupedItemInvView}. */
    default GroupedItemInvView getGroupedView() {
        GroupedItemInvView real = this;
        return new GroupedItemInvView() {
            @Override
            public GroupedItemInvView getGroupedView() {
                return this;
            }

            @Override
            public int getAmount(ItemStack stack) {
                return real.getAmount(stack);
            }

            @Override
            public int getAmount(ItemFilter filter) {
                return real.getAmount(filter);
            }

            @Override
            public int getCapacity(ItemStack stack) {
                return real.getCapacity(stack);
            }

            @Override
            public int getSpace(ItemStack stack) {
                return real.getSpace(stack);
            }

            @Override
            public int getTotalCapacity() {
                return real.getTotalCapacity();
            }

            @Override
            public Set<ItemStack> getStoredStacks() {
                // Just in case this is actually a modifiable set
                // (for example if the real implementation uses a Map<ItemStack, amount>)
                return Collections.unmodifiableSet(real.getStoredStacks());
            }

            @Override
            public ItemInvStatistic getStatistics(ItemFilter filter) {
                return real.getStatistics(filter);
            }

            @Override
            public int getChangeValue() {
                return real.getChangeValue();
            }

            @Override
            public ListenerToken addListener(InvMarkDirtyListener listener, ListenerRemovalToken removalToken) {
                final GroupedItemInvView view = this;
                return real.addListener(
                    (inv) -> {
                        // Defend against giving the listener the real (possibly changeable) inventory.
                        // In addition the listener would probably cache *this view* rather than the backing inventory
                        // so they most likely need it to be this inventory.
                        listener.onMarkDirty(view);
                    }, removalToken
                );
            }

            @Override
            public ListenerToken addListener(ItemInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
                final GroupedItemInvView view = this;
                return real.addListener(
                    (inv, stack, prev, curr) -> {
                        // Defend against giving the listener the real (possibly changeable) inventory.
                        // In addition the listener would probably cache *this view* rather than the backing inventory
                        // so they most likely need it to be this inventory.
                        listener.onChange(view, stack, prev, curr);
                    }, removalToken
                );
            }
        };
    }

    /** Statistics associated with a single {@link ItemFilter} in a given inventory. */
    public static final class ItemInvStatistic {

        public final ItemFilter filter;

        /** The total amount of the given filter. */
        public final int amount;

        /** The total amount of space that the given filter can be added to (where an inventory already contains a
         * partial stack). */
        public final int spaceAddable;

        /** The total amount of additional entries that could be added to by this filter. This might be -1 if the filter
         * isn't specific enough to properly calculate this value. */
        public final int spaceTotal;

        public ItemInvStatistic(ItemFilter filter, int amount, int spaceAddable, int spaceTotal) {
            this.filter = filter;
            this.amount = amount;
            this.spaceAddable = spaceAddable;
            this.spaceTotal = spaceTotal;
        }
    }
}
