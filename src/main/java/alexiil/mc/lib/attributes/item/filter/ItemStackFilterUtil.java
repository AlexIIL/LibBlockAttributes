package alexiil.mc.lib.attributes.item.filter;

import alexiil.mc.lib.attributes.util.LibBlockAttributes;

public enum ItemStackFilterUtil {
    ;

    /** Attempts to find the maximum stack amount from all of the stacks in the filter. */
    public static int findMaximumStackAmount(IItemFilter filter) {
        if (filter == IItemFilter.ANY_STACK) {
            return 64;
        }
        if (filter == IItemFilter.NOTHING) {
            return 0;
        }

        if (filter instanceof ExactItemStackFilter) {
            return ((ExactItemStackFilter) filter).stack.getMaxAmount();
        }

        if (filter instanceof AggregateStackFilter) {
            int max = 1;
            for (IItemFilter inner : (AggregateStackFilter) filter) {
                max = Math.max(max, findMaximumStackAmount(inner));
            }
            return max;
        }

        if (filter instanceof IReadableItemFilter) {
            LibBlockAttributes.LOGGER.warn("Encountered an unknown readable filter " + filter.getClass()
                + " - ItemStackFilterUtil.findMaximumStackAmount should probably have support for it!");
        }
        return 64;
    }
}
