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
     * {@link ItemStack#getAmount()}.
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

    default Predicate<ItemStack> asPredicate() {
        final ItemFilter filter = this;
        return new Predicate<ItemStack>() {
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
                } else {
                    return Predicate.super.and(other);
                }
            }

            @Override
            public Predicate<ItemStack> or(Predicate<? super ItemStack> other) {
                if (other instanceof ItemFilter) {
                    // Because the real filter might have optimisations in or()
                    return filter.or((ItemFilter) other).asPredicate();
                } else {
                    return Predicate.super.and(other);
                }
            }
        };
    }
}
