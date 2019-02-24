package alexiil.mc.lib.attributes.item.impl;

import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.IFixedItemInvView;
import alexiil.mc.lib.attributes.item.IItemInvStats;
import alexiil.mc.lib.attributes.item.ItemStackCollections;
import alexiil.mc.lib.attributes.item.filter.AggregateStackFilter;
import alexiil.mc.lib.attributes.item.filter.IStackFilter;
import alexiil.mc.lib.attributes.item.filter.StackFilterUtil;

public final class SimpleFixedItemInvStats implements IItemInvStats {

    private final IFixedItemInvView inv;

    public SimpleFixedItemInvStats(IFixedItemInvView inv) {
        this.inv = inv;
    }

    @Override
    public ItemInvStatistic getStatistics(IStackFilter filter) {
        int amount = 0;
        int space = 0;
        int totalSpace = 0;
        boolean totalSpaceValid = true;
        for (int s = 0; s < inv.getInvSize(); s++) {
            ItemStack stack = inv.getInvStack(s);
            if (!stack.isEmpty()) {
                if (filter.matches(stack)) {
                    amount += stack.getAmount();
                    int max = inv.getMaxAmount(s, stack);
                    space += max - stack.getAmount();
                }
                continue;
            }
            IStackFilter realFilter = AggregateStackFilter.and(filter, inv.getFilterForSlot(s));
            int max = StackFilterUtil.findMaximumStackAmount(realFilter);
            max = Math.min(max, inv.getMaxAmount(s, stack));
            if (max < 0) {
                // Nothing we can do
                totalSpaceValid = true;
            } else {
                totalSpace += max;
            }
        }
        return new ItemInvStatistic(filter, amount, space, totalSpaceValid ? totalSpace : -1);
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        Set<ItemStack> set = ItemStackCollections.set();
        for (int s = 0; s < inv.getInvSize(); s++) {
            ItemStack stack = inv.getInvStack(s);
            if (!stack.isEmpty()) {
                set.add(stack);
            }
        }
        return set;
    }
}
