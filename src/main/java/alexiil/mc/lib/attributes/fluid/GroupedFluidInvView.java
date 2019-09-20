/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.util.Collections;
import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.item.GroupedItemInvView;

///** Provides general statistics for any permanent fluid storing inventory. The inventory in question doesn't have to be
// * an {@link IFixedFluidInvView}. */
public interface GroupedFluidInvView {

    /** @return a set containing all of the {@link FluidKey}'s that are stored in the inventory. */
    Set<FluidKey> getStoredFluids();

    /** @param fluid The stack to check for. Must not be {@link ItemStack#isEmpty() empty}.
     * @return The total amount of the given stack that is stored in this inventory. */
    default int getAmount(FluidKey fluid) {
        FluidInvStatistic stats = getStatistics(new ExactFluidFilter(fluid));
        assert stats.spaceTotal >= 0;
        return stats.amount;
    }

    /** @param fluid The fluid to check for. Cannot be the empty fluid.
     * @return The total space that is available (right now!) to store the given stack. */
    default int getCapacity(FluidKey fluid) {
        FluidInvStatistic stats = getStatistics(new ExactFluidFilter(fluid));
        assert stats.spaceTotal >= 0;
        return stats.amount + stats.spaceAddable + stats.spaceTotal;
    }

    /** @return The total capacity for every {@link FluidKey} that can be stored in this inventory.
     *         <p>
     *         NOTE: This value might <i>not</i> be equal to the sum of {@link #getCapacity(FluidKey)} over
     *         {@link #getStoredFluids()}! */
    int getTotalCapacity();

    /** @return The total space that could store the given stack, not including space that is currently taken up by the
     *         stack (so this should change with {@link #getAmount(FluidKey)}). */
    default int getSpace(FluidKey fluid) {
        return getCapacity(fluid) - getAmount(fluid);
    }

    /** @param filter The filter to check on.
     * @return Statistics about the currently stored amount, capacity, and space for everything that matches the given
     *         filter. */
    FluidInvStatistic getStatistics(FluidFilter filter);

    /** @param filter The {@link FluidKey} to check for.
     * @return Statistics about the currently stored amount, capacity, and space for everything that matches the given
     *         item stack. */
    default FluidInvStatistic getStatistics(FluidKey filter) {
        return getStatistics(new ExactFluidFilter(filter));
    }

    /** @return A count of all the {@link FluidKey}'s that match the given filter. */
    default int getAmount(FluidFilter filter) {
        return getStatistics(filter).amount;
    }

    /** Adds the given listener to this inventory, such that the
     * {@link FluidInvAmountChangeListener#onChange(GroupedFluidInvView, FluidKey, int, int)} will be called every time
     * that this inventory changes. However if this inventory doesn't support listeners then this will return a null
     * {@link ListenerToken token}.
     * 
     * @param removalToken A token that will be called whenever the given listener is removed from this inventory (or if
     *            this inventory itself is unloaded or otherwise invalidated).
     * @return A token that represents the listener, or null if the listener could not be added. */
    ListenerToken addListener(FluidInvAmountChangeListener listener, ListenerRemovalToken removalToken);

    /** @return A completely unmodifiable view of this {@link GroupedItemInvView}. */
    default GroupedFluidInvView getView() {
        GroupedFluidInvView real = this;
        return new GroupedFluidInvView() {
            @Override
            public GroupedFluidInvView getView() {
                return this;
            }

            @Override
            public int getAmount(FluidKey fluid) {
                return real.getAmount(fluid);
            }

            @Override
            public int getAmount(FluidFilter filter) {
                return real.getAmount(filter);
            }

            @Override
            public int getCapacity(FluidKey fluid) {
                return real.getCapacity(fluid);
            }

            @Override
            public int getSpace(FluidKey fluid) {
                return real.getSpace(fluid);
            }

            @Override
            public int getTotalCapacity() {
                return real.getTotalCapacity();
            }

            @Override
            public Set<FluidKey> getStoredFluids() {
                // Just in case this is actually a modifiable set
                // (for example if the real implementation uses a Map<FluidKey, FluidVolume>)
                return Collections.unmodifiableSet(real.getStoredFluids());
            }

            @Override
            public FluidInvStatistic getStatistics(FluidFilter filter) {
                return real.getStatistics(filter);
            }

            @Override
            public ListenerToken addListener(FluidInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
                final GroupedFluidInvView view = this;
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

    /** Statistics associated with a single {@link FluidFilter} in a given inventory. */
    public static final class FluidInvStatistic {

        public final FluidFilter filter;

        /** The total amount of the given filter. */
        public final int amount;

        /** The total amount of space that the given filter can be added to (where an inventory already contains a
         * partial stack). */
        public final int spaceAddable;

        /** The total amount of additional entries that could be added to by this filter. This might be -1 if the filter
         * isn't specific enough to properly calculate this value. */
        public final int spaceTotal;

        public FluidInvStatistic(FluidFilter filter, int amount, int spaceAddable, int spaceTotal) {
            this.filter = filter;
            this.amount = amount;
            this.spaceAddable = spaceAddable;
            this.spaceTotal = spaceTotal;
        }

        /** @return A new {@link FluidInvStatistic} that has 0's for {@link #amount}, {@link #spaceAddable}, and
         *         {@link #spaceTotal} */
        public static FluidInvStatistic emptyOf(FluidFilter filter) {
            return new FluidInvStatistic(filter, 0, 0, 0);
        }
    }
}
