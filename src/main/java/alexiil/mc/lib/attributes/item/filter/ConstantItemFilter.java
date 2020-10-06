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

    @Deprecated // (since = "0.8.0", forRemoval = true)
    ANYTHING_EXCEPT_EMPTY(true, false),

    @Deprecated // (since = "0.8.0", forRemoval = true)
    ONLY_EMPTY(false, true),

    NOTHING(false, false);

    private final boolean fullResult;

    private ConstantItemFilter(boolean fullResult, boolean emptyResult) {
        this.fullResult = fullResult;
    }

    public static ConstantItemFilter of(boolean result) {
        return result ? ANYTHING : NOTHING;
    }

    @Deprecated // (since = "0.8.0", forRemoval = true)
    public static ConstantItemFilter of(boolean fullResult, boolean emptyResult) {
        if (fullResult) {
            return emptyResult ? ANYTHING : ANYTHING_EXCEPT_EMPTY;
        } else {
            return emptyResult ? ONLY_EMPTY : NOTHING;
        }
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.isEmpty() ? false : fullResult;
    }

    @Override
    public ItemFilter negate() {
        return of(!fullResult);
    }

    @Override
    public ItemFilter and(ItemFilter other) {
        return fullResult ? other : NOTHING;
    }

    @Override
    public ItemFilter or(ItemFilter other) {
        return fullResult ? ANYTHING : other;
    }

    // Don't override asPredicate so that we still get the better version that calls our own negate(), and(), or()
    // methods.
}
