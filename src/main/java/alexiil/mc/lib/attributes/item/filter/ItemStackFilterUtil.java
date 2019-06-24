/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

public enum ItemStackFilterUtil {
    ;

    /** Attempts to find the maximum stack amount from all of the stacks in the filter. */
    public static int findMaximumStackAmount(ItemFilter filter) {
        if (filter == ConstantItemFilter.ANYTHING) {
            return 64;
        }
        if (filter == ConstantItemFilter.NOTHING) {
            return 0;
        }

        if (filter instanceof ExactItemStackFilter) {
            return ((ExactItemStackFilter) filter).stack.getMaxAmount();
        }

        if (filter instanceof AggregateItemFilter) {
            int max = 1;
            for (ItemFilter inner : (AggregateItemFilter) filter) {
                max = Math.max(max, findMaximumStackAmount(inner));
            }
            return max;
        }

        if (filter instanceof ReadableItemFilter) {
            LibBlockAttributes.LOGGER.warn("Encountered an unknown readable filter " + filter.getClass()
                + " - ItemStackFilterUtil.findMaximumStackAmount should probably have support for it!");
        }
        return 64;
    }
}
