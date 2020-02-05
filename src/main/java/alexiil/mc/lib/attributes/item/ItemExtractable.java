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
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ExactItemStackFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.impl.FilteredItemExtractable;

/** Defines an object that can have items extracted from it. */
@FunctionalInterface
public interface ItemExtractable {

    /** Attempt to extract *any* {@link ItemStack} from this that {@link ItemFilter#matches(ItemStack) matches} the
     * given {@link ItemFilter}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @param simulation If {@link Simulation#SIMULATE} then this should return the same result that a call with
     *            {@link Simulation#ACTION} would do, but without modifying anything else.
     * @return A new, independent {@link ItemStack} that was extracted. */
    ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation);

    /** Calls {@link #attemptExtraction(ItemFilter, int, Simulation) attemptExtraction()} with an {@link ItemFilter} of
     * {@link ConstantItemFilter#ANYTHING}. */
    default ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
        return attemptExtraction(ConstantItemFilter.ANYTHING, maxAmount, simulation);
    }

    /** Attempt to extract *any* {@link ItemStack} from this that {@link ItemFilter#matches(ItemStack) matches} the
     * given {@link ItemFilter}.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(ItemFilter, int, Simulation)} with a {@link Simulation}
     * parameter of {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted. */
    default ItemStack extract(ItemFilter filter, int maxAmount) {
        return attemptExtraction(filter, maxAmount, Simulation.ACTION);
    }

    /** Attempt to extract *any* {@link ItemStack} from this that is
     * {@link ItemStackUtil#areEqualIgnoreAmounts(ItemStack, ItemStack) equal}to the given {@link ItemStack}.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(ItemFilter, int, Simulation)} with an {@link ItemFilter}
     * parameter of {@link ExactItemStackFilter} and a {@link Simulation} parameter of {@link Simulation#ACTION ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted. */
    default ItemStack extract(ItemStack filter, int maxAmount) {
        return attemptExtraction(new ExactItemStackFilter(filter), maxAmount, Simulation.ACTION);
    }

    /** Attempt to extract *any* {@link ItemStack} from this.
     * <p>
     * This is equivalent to calling {@link #attemptExtraction(ItemFilter, int, Simulation)} with an {@link ItemFilter}
     * parameter of {@link ConstantItemFilter#ANYTHING} and a {@link Simulation} parameter of {@link Simulation#ACTION
     * ACTION}.
     * 
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @return A new, independent {@link ItemStack} that was extracted. */
    default ItemStack extract(int maxAmount) {
        return attemptExtraction(ConstantItemFilter.ANYTHING, maxAmount, Simulation.ACTION);
    }

    /** @return A new {@link ItemExtractable} that has an additional filter applied to limit the items extracted from
     *         it. */
    default ItemExtractable filtered(ItemFilter filter) {
        return new FilteredItemExtractable(this, filter);
    }

    /** @return An object that only implements {@link ItemExtractable}, and does not expose any of the other
     *         modification methods that sibling or subclasses offer (like {@link ItemInsertable} or
     *         {@link GroupedItemInv}. */
    default ItemExtractable getPureExtractable() {
        ItemExtractable delegate = this;
        return new ItemExtractable() {
            @Override
            public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
                return delegate.attemptExtraction(filter, maxAmount, simulation);
            }

            @Override
            public ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
                return delegate.attemptAnyExtraction(maxAmount, simulation);
            }
        };
    }
}
