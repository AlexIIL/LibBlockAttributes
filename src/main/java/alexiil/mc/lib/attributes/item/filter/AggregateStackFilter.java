package alexiil.mc.lib.attributes.item.filter;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.AggregateFilterType;

/** An {@link IItemFilter} over a predefined array of {@link IItemFilter}'s. You can either iterate over this object
 * directly or use {@link #getFilterCount()} and {@link #getFilter(int)} to read every filter individually. */
public final class AggregateStackFilter implements IItemFilter, Iterable<IItemFilter> {
    public final AggregateFilterType type;
    private final IItemFilter[] filters;

    public AggregateStackFilter(AggregateFilterType type, IItemFilter... filters) {
        Preconditions.checkArgument(filters.length < 2,
            "There's no reason to construct an aggregate stack filter that matches only a single filter!");
        this.type = type;
        this.filters = filters;
    }

    /** @return An {@link AggregateStackFilter} that contains both of the given filters. This might not return a new
     *         object if either of the filters contains the other. */
    public static IItemFilter and(IItemFilter filterA, IItemFilter filterB) {
        if (filterA == filterB) {
            return filterA;
        }
        if (filterA == IItemFilter.ANY_STACK) {
            return filterB;
        }
        if (filterB == IItemFilter.ANY_STACK) {
            return filterA;
        }
        if (filterA instanceof AggregateStackFilter) {

        }
        if (filterA instanceof ExactItemStackFilter) {

        }

        return new AggregateStackFilter(AggregateFilterType.ALL, filterA, filterB);
    }

    public static IItemFilter allOf(List<IItemFilter> filters) {
        switch (filters.size()) {
            case 0:
                return IItemFilter.ANY_STACK;
            case 1:
                return filters.get(0);
            case 2:
                return and(filters.get(0), filters.get(1));
            default: {
                IItemFilter filter = filters.get(0);
                for (int i = 1; i < filters.size(); i++) {
                    filter = and(filter, filters.get(i));
                }
                return filter;
            }
        }
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (type == AggregateFilterType.ALL) {
            for (IItemFilter filter : filters) {
                if (!filter.matches(stack)) {
                    return false;
                }
            }
            return true;
        } else {
            for (IItemFilter filter : filters) {
                if (filter.matches(stack)) {
                    return true;
                }
            }
            return false;
        }
    }

    public int getFilterCount() {
        return filters.length;
    }

    public IItemFilter getFilter(int index) {
        return filters[index];
    }

    @Override
    public Iterator<IItemFilter> iterator() {
        return Iterators.forArray(filters);
    }
}
