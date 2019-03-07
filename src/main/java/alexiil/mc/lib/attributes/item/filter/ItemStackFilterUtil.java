package alexiil.mc.lib.attributes.item.filter;

import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

public enum ItemStackFilterUtil {
    ;

    /** Attempts to find the maximum stack amount from all of the stacks in the filter. */
    public static int findMaximumStackAmount(IItemFilter filter) {
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
            for (IItemFilter inner : (AggregateItemFilter) filter) {
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
