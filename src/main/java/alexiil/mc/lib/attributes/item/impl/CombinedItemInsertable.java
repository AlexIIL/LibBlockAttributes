package alexiil.mc.lib.attributes.item.impl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IItemInsertable;
import alexiil.mc.lib.attributes.item.filter.AggregateItemFilter;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;

public final class CombinedItemInsertable implements IItemInsertable {

    private final List<? extends IItemInsertable> insertables;
    private final IItemFilter filter;

    public CombinedItemInsertable(List<? extends IItemInsertable> list) {
        List<IItemFilter> filters = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            filters.add(list.get(i).getInsertionFilter());
        }
        this.filter = AggregateItemFilter.allOf(filters);
        this.insertables = list;
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        for (IItemInsertable insertable : insertables) {
            stack = insertable.attemptInsertion(stack, simulation);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    @Override
    public IItemFilter getInsertionFilter() {
        return filter;
    }
}
