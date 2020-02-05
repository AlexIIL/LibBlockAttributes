/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import java.util.function.Predicate;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** A general {@link Predicate} interface for {@link ItemStack}s. *
 * <p>
 * 5 basic implementations are provided:
 * <ul>
 * <li>{@link ConstantItemFilter} for filters that always match or never match.</li>
 * <li>{@link ExactItemFilter} for a filter that matches on a single {@link Item}, and rejects others.</li>
 * <li>{@link ExactItemStackFilter} for a filter that matches on a single {@link ItemStack} (using .</li>
 * <li>{@link ItemClassFilter} for a filter that only matches on {@link Item}s whose {@link #getClass()}
 * {@link Class#isInstance(Object) is an instance} of the class specified by the filter.</li>
 * <li>{@link AggregateItemFilter} for a filter that either AND's or OR's several other {@link ItemFilter}'s into
 * one.</li>
 * </ul>
 */
@FunctionalInterface
public interface ItemFilter {

    /** Checks to see if this filter matches the given stack. Note that this must not care about
     * {@link ItemStack#getCount()}, except in the case where the stack is {@link ItemStack#isEmpty()}. */
    boolean matches(ItemStack stack);

    default ItemFilter negate() {
        return new InvertedItemFilter(this);
    }

    default ItemFilter and(ItemFilter other) {
        return AggregateItemFilter.and(this, other);
    }

    default ItemFilter or(ItemFilter other) {
        return AggregateItemFilter.or(this, other);
    }

    /** @return A {@link Predicate} that delegates to this {@link ItemFilter#matches(ItemStack)}. The default
     *         implementation returns {@link ItemFilterAsPredicate}, which is <i>probably</i> always what you want. */
    default Predicate<ItemStack> asPredicate() {
        return new ItemFilterAsPredicate(this);
    }

    /** {@link Predicate} wrapper around an {@link ItemFilter}. This always returns false for null or empty stacks. */
    public static final class ItemFilterAsPredicate implements Predicate<ItemStack> {
        public final ItemFilter filter;

        public ItemFilterAsPredicate(ItemFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean test(ItemStack stack) {
            if (stack == null) {
                // Predicate.test doesn't have this restriction
                return false;
            }
            return filter.matches(stack);
        }

        @Override
        public Predicate<ItemStack> negate() {
            // Because the real filter might have optimisations in negate()
            return filter.negate().asPredicate();
        }

        @Override
        public Predicate<ItemStack> and(Predicate<? super ItemStack> other) {
            if (other instanceof ItemFilter) {
                // Because the real filter might have optimisations in and()
                return filter.and((ItemFilter) other).asPredicate();
            } else if (other instanceof ItemFilterAsPredicate) {
                return filter.and(((ItemFilterAsPredicate) other).filter).asPredicate();
            } else {
                return Predicate.super.and(other);
            }
        }

        @Override
        public Predicate<ItemStack> or(Predicate<? super ItemStack> other) {
            if (other instanceof ItemFilter) {
                // Because the real filter might have optimisations in or()
                return filter.or((ItemFilter) other).asPredicate();
            } else if (other instanceof ItemFilterAsPredicate) {
                return filter.or(((ItemFilterAsPredicate) other).filter).asPredicate();
            } else {
                return Predicate.super.and(other);
            }
        }
    }
}
