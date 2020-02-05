/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.ItemInsertableFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.impl.FilteredItemInsertable;
import alexiil.mc.lib.attributes.misc.LimitedConsumer;

/** Defines an object that can have items inserted into it. */
@FunctionalInterface
public interface ItemInsertable extends LimitedConsumer<ItemStack> {

    /** Inserts the given stack into this insertable, and returns the excess.
     * 
     * @param stack The incoming stack. Must not be modified by this call.
     * @param simulation If {@link Simulation#SIMULATE} then this shouldn't modify anything.
     * @return the excess {@link ItemStack} that wasn't accepted. This will be independent of this insertable, however
     *         it might be the given stack instead of a completely new object. */
    ItemStack attemptInsertion(ItemStack stack, Simulation simulation);

    /** @deprecated This is an override for {@link LimitedConsumer}, for the full javadoc you probably want to call
     *             {@link #attemptInsertion(ItemStack, Simulation)} directly. */
    @Override
    @Deprecated
    default boolean offer(ItemStack object, Simulation simulation) {
        return attemptInsertion(object, simulation).isEmpty();
    }

    /** @deprecated This is an override for {@link LimitedConsumer}, for the full javadoc you probably want to call
     *             {@link #insert(ItemStack)} directly. */
    @Override
    @Deprecated
    default boolean offer(ItemStack object) {
        return insert(object).isEmpty();
    }

    /** @deprecated This is an override for {@link LimitedConsumer}, for the full javadoc you probably want to call
     *             {@link #attemptInsertion(ItemStack, Simulation) #attemptInsertion}(ItemStack, Simulation.SIMULATE)
     *             directly. */
    @Override
    @Deprecated
    default boolean wouldAccept(ItemStack object) {
        return attemptInsertion(object, Simulation.SIMULATE).isEmpty();
    }

    /** Inserts the given stack into this insertable, and returns the excess.
     * <p>
     * This is equivalent to calling {@link #attemptInsertion(ItemStack, Simulation)} with a {@link Simulation}
     * parameter of {@link Simulation#ACTION ACTION}.
     * 
     * @param stack The incoming stack. Must not be modified by this call.
     * @return the excess {@link ItemStack} that wasn't accepted. This will be independent of this insertable, however
     *         it might be the given stack instead of a completely new object. */
    default ItemStack insert(ItemStack stack) {
        return attemptInsertion(stack, Simulation.ACTION);
    }

    /** Returns an {@link ItemFilter} to determine if {@link #attemptInsertion(ItemStack, Simulation)} will accept a
     * stack. The default implementation is a call to {@link #attemptInsertion(ItemStack, Simulation)
     * attemptInsertion}(stack, {@link Simulation#SIMULATE}), and it is only useful to override this if the resulting
     * filter contains information that might be usable by the caller.
     * 
     * @return A filter to determine if {@link #attemptInsertion(ItemStack, Simulation)} will accept the entirety of a
     *         given stack. */
    default ItemFilter getInsertionFilter() {
        return new ItemInsertableFilter(this);
    }

    default ItemInsertable filtered(ItemFilter filter) {
        return new FilteredItemInsertable(this, filter);
    }

    /** @return An object that only implements {@link ItemInsertable}, and does not expose any of the other modification
     *         methods that sibling or subclasses offer (like {@link ItemExtractable} or {@link GroupedItemInv}. */
    default ItemInsertable getPureInsertable() {
        final ItemInsertable delegate = this;
        return new ItemInsertable() {
            @Override
            public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
                return delegate.attemptInsertion(stack, simulation);
            }

            @Override
            public ItemFilter getInsertionFilter() {
                return delegate.getInsertionFilter();
            }
        };
    }
}
