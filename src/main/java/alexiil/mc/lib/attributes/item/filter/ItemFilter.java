/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

/** A general {@link Predicate} interface for {@link ItemStack}s. */
@FunctionalInterface
public interface ItemFilter {

    /** Checks to see if the given filter matches the given stack. Note that this must not care about
     * {@link ItemStack#getCount()}.
     * 
     * @throws IllegalArgumentException if the given {@link ItemStack} is {@link ItemStack#isEmpty() empty}. */
    boolean matches(ItemStack stack);

    default ItemFilter negate() {
        return stack -> !this.matches(stack);
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
            if (stack == null || stack.isEmpty()) {
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
