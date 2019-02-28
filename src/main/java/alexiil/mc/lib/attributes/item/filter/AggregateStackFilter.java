package alexiil.mc.lib.attributes.item.filter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.AggregateFilterType;

/** An {@link IItemFilter} over a predefined array of {@link IItemFilter}'s. You can either iterate over this object
 * directly or use {@link #getFilterCount()} and {@link #getFilter(int)} to read every filter individually. */
public final class AggregateStackFilter implements IReadableItemFilter, Iterable<IItemFilter> {
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
        return combine(AggregateFilterType.ALL, filterA, filterB);
    }

    /** @return An {@link AggregateStackFilter} that contains both of the given filters. This might not return a new
     *         object if either of the filters contains the other. */
    public static IItemFilter or(IItemFilter filterA, IItemFilter filterB) {
        return combine(AggregateFilterType.ANY, filterA, filterB);
    }

    public static IItemFilter combine(AggregateFilterType type, IItemFilter filterA, IItemFilter filterB) {
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

        return new AggregateStackFilter(type, filterA, filterB);
    }

    public static IItemFilter allOf(IItemFilter... filters) {
        return combine(AggregateFilterType.ALL, filters);
    }

    public static IItemFilter anyOf(IItemFilter... filters) {
        return combine(AggregateFilterType.ANY, filters);
    }

    public static IItemFilter combine(AggregateFilterType type, IItemFilter... filters) {
        return combine(type, Arrays.asList(filters));
    }

    public static IItemFilter allOf(List<IItemFilter> filters) {
        return combine(AggregateFilterType.ALL, filters);
    }

    public static IItemFilter anyOf(List<IItemFilter> filters) {
        return combine(AggregateFilterType.ANY, filters);
    }

    public static IItemFilter combine(AggregateFilterType type, List<IItemFilter> filters) {
        if (!(filters instanceof RandomAccess)) {
            filters = Arrays.asList(filters.toArray(new IItemFilter[0]));
        }
        switch (filters.size()) {
            case 0:
                return IItemFilter.ANY_STACK;
            case 1:
                return filters.get(0);
            case 2:
                // I'm assuming this might be faster than putting everything into a list?
                return combine(type, filters.get(0), filters.get(1));
            default: {
                IItemFilter filter = filters.get(0);
                for (int i = 1; i < filters.size(); i++) {
                    filter = combine(type, filter, filters.get(i));
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
