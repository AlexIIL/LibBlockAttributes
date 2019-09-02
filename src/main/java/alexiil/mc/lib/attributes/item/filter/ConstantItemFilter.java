/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import net.minecraft.item.ItemStack;

public enum ConstantItemFilter implements ReadableItemFilter {
    ANYTHING(true, true),
    ANYTHING_EXCEPT_EMPTY(true, false),
    ONLY_EMPTY(false, true),
    NOTHING(false, false);

    private final boolean fullResult, emptyResult;

    private ConstantItemFilter(boolean fullResult, boolean emptyResult) {
        this.fullResult = fullResult;
        this.emptyResult = emptyResult;
    }

    public static ConstantItemFilter of(boolean result) {
        return result ? ANYTHING : NOTHING;
    }

    public static ConstantItemFilter of(boolean fullResult, boolean emptyResult) {
        if (fullResult) {
            return emptyResult ? ANYTHING : ANYTHING_EXCEPT_EMPTY;
        } else {
            return emptyResult ? ONLY_EMPTY : NOTHING;
        }
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.isEmpty() ? emptyResult : fullResult;
    }

    @Override
    public ItemFilter negate() {
        return of(!fullResult, !emptyResult);
    }

    @Override
    public ItemFilter and(ItemFilter other) {
        if (fullResult && emptyResult) {
            return other;
        }
        if (!fullResult && !emptyResult) {
            return NOTHING;
        }

        boolean otherMatchesEmpty = other.matches(ItemStack.EMPTY);

        if (otherMatchesEmpty) {
            if (fullResult) {
                return ReadableItemFilter.super.and(other);
            } else {
                return ONLY_EMPTY;
            }
        } else {
            if (fullResult) {
                return other;
            } else {
                return NOTHING;
            }
        }
    }

    @Override
    public ItemFilter or(ItemFilter other) {

        if (fullResult && emptyResult) {
            return ANYTHING;
        }
        if (!fullResult && !emptyResult) {
            return other;
        }

        boolean otherMatchesEmpty = other.matches(ItemStack.EMPTY);

        if (otherMatchesEmpty) {
            if (fullResult) {
                return ANYTHING;
            } else {
                return other;
            }
        } else {
            if (fullResult) {
                return other;
            } else {
                return ReadableItemFilter.super.or(other);
            }
        }
    }

    // Don't override asPredicate so that we still get the better version that calls our own negate(), and(), or()
    // methods.
}
