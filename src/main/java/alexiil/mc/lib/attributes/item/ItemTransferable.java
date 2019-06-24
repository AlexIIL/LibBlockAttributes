/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** Combined interface for {@link ItemInsertable} and {@link ItemExtractable}. (This is provided for simplicity when
 * exposing inventories for modification but when you don't want to implement the full set of methods that
 * {@link GroupedItemInv} provides). */
public interface ItemTransferable extends ItemInsertable, ItemExtractable {

    /** @return A new {@link ItemTransferable} that will insert into the given insertable, and never return any items
     *         from {@link #attemptExtraction(ItemFilter, int, Simulation)}. */
    @Nonnull
    public static ItemTransferable from(ItemInsertable insertable) {
        return new ItemTransferable() {

            @Override
            public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
                return insertable.attemptInsertion(stack, simulation);
            }

            @Override
            public ItemFilter getInsertionFilter() {
                return insertable.getInsertionFilter();
            }

            @Override
            public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
                return ItemStack.EMPTY;
            }
        };
    }

    /** @return A new {@link ItemTransferable} that will extract from the given extractable, and reject every inserted
     *         stack. */
    @Nonnull
    public static ItemTransferable from(ItemExtractable extractable) {
        return new ItemTransferable() {

            @Override
            public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
                return stack;
            }

            @Override
            public ItemFilter getInsertionFilter() {
                return ConstantItemFilter.NOTHING;
            }

            @Override
            public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
                return extractable.attemptExtraction(filter, maxAmount, simulation);
            }

            @Override
            public ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
                return extractable.attemptAnyExtraction(maxAmount, simulation);
            }
        };
    }

    /** @return A new {@link ItemTransferable} that will extract from the given extractable, and insert into the given
     *         extractable. */
    @Nonnull
    public static ItemTransferable from(ItemInsertable insertable, ItemExtractable extractable) {
        if (insertable == extractable && insertable instanceof ItemTransferable) {
            return (ItemTransferable) insertable;
        }
        return new ItemTransferable() {
            @Override
            public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
                return insertable.attemptInsertion(stack, simulation);
            }

            @Override
            public ItemFilter getInsertionFilter() {
                return insertable.getInsertionFilter();
            }

            @Override
            public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
                return extractable.attemptExtraction(filter, maxAmount, simulation);
            }

            @Override
            public ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
                return extractable.attemptAnyExtraction(maxAmount, simulation);
            }
        };
    }
}
