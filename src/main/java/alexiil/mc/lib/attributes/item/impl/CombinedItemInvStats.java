package alexiil.mc.lib.attributes.item.impl;

import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.IItemInvStats;
import alexiil.mc.lib.attributes.item.ItemStackCollections;
import alexiil.mc.lib.attributes.item.filter.IStackFilter;

public class CombinedItemInvStats implements IItemInvStats {

    private final List<? extends IItemInvStats> statistics;

    public CombinedItemInvStats(List<? extends IItemInvStats> statistics) {
        this.statistics = statistics;
    }

    @Override
    public ItemInvStatistic getStatistics(IStackFilter filter) {
        int amount = 0;
        int spaceAddable = 0;
        int spaceTotal = 0;
        for (IItemInvStats stats : statistics) {
            ItemInvStatistic stat = stats.getStatistics(filter);
            amount += stat.amount;
            spaceAddable += stat.spaceAddable;
            spaceTotal += stat.spaceTotal;
        }
        return new ItemInvStatistic(filter, amount, spaceAddable, spaceTotal);
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        Set<ItemStack> set = ItemStackCollections.set();
        for (IItemInvStats stats : statistics) {
            set.addAll(stats.getStoredStacks());
        }
        return set;
    }
}
