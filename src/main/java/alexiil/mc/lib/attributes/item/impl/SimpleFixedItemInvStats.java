package alexiil.mc.lib.attributes.item.impl;

import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvStats;
import alexiil.mc.lib.attributes.item.ItemStackCollections;
import alexiil.mc.lib.attributes.item.filter.AggregateItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemStackFilterUtil;

public final class SimpleFixedItemInvStats implements ItemInvStats {

    private final FixedItemInvView inv;

    public SimpleFixedItemInvStats(FixedItemInvView inv) {
        this.inv = inv;
    }

    @Override
    public ItemInvStatistic getStatistics(ItemFilter filter) {
        int amount = 0;
        int space = 0;
        int totalSpace = 0;
        boolean totalSpaceValid = true;
        for (int s = 0; s < inv.getSlotCount(); s++) {
            ItemStack stack = inv.getInvStack(s);
            if (!stack.isEmpty()) {
                if (filter.matches(stack)) {
                    amount += stack.getAmount();
                    int max = inv.getMaxAmount(s, stack);
                    space += max - stack.getAmount();
                }
                continue;
            }
            ItemFilter realFilter = AggregateItemFilter.and(filter, inv.getFilterForSlot(s));
            // FIXME: I think this next bit might be a bit broken?
            int max = ItemStackFilterUtil.findMaximumStackAmount(realFilter);
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
        for (int s = 0; s < inv.getSlotCount(); s++) {
            ItemStack stack = inv.getInvStack(s);
            if (!stack.isEmpty()) {
                set.add(stack);
            }
        }
        return set;
    }
}
