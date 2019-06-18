package alexiil.mc.lib.attributes.item.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import alexiil.mc.lib.attributes.AggregateFilterType;

/** An {@link ItemFilter} that only matches on a single {@link Item}. */
public final class ExactItemFilter implements ReadableItemFilter {

    public final Item item;

    public ExactItemFilter(Item item) {
        this.item = item;
    }

    /** @return Either {@link ConstantItemFilter#NOTHING} if {@link ItemConvertible#asItem()} returns {@link Items#AIR},
     *         or an {@link ExactItemFilter} if it returns any other {@link Item}. */
    public static ReadableItemFilter createFilter(ItemConvertible entry) {
        Item item = entry.asItem();
        if (item == Items.AIR) {
            return ConstantItemFilter.NOTHING;
        }
        return new ExactItemFilter(item);
    }

    public static ReadableItemFilter anyOf(Collection<? extends ItemConvertible> items) {
        return anyOf(items.toArray(new ItemConvertible[0]));
    }

    public static ReadableItemFilter anyOf(ItemConvertible[] items) {
        if (items.length == 0) {
            return ConstantItemFilter.NOTHING;
        } else if (items.length == 1) {
            return createFilter(items[0]);
        } else {
            List<ReadableItemFilter> filters = new ArrayList<>();
            for (int i = 0; i < items.length; i++) {
                ReadableItemFilter filter = createFilter(items[i]);
                if (filter != ConstantItemFilter.NOTHING) {
                    filters.add(filter);
                }
            }
            if (filters.isEmpty()) {
                return ConstantItemFilter.NOTHING;
            } else if (filters.size() == 1) {
                return filters.get(0);
            }
            return new AggregateItemFilter(AggregateFilterType.ANY, filters.toArray(new ItemFilter[0]));
        }
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.getItem() == item;
    }
}
